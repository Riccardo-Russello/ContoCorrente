package it.russello.contocorrente.controller;

import com.jayway.jsonpath.JsonPath;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private JwtEncoder jwtEncoder;
    private void savePerson(){
        Person person = new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15));
        person.setUsername("mario.rossi");
        person.setPassword("passwmario");
        personRepository.saveAndFlush(person);
    }
    private String loginAndGetToken() throws Exception{
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "username" : "mario.rossi",
                            "password" : "passwmario"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    @Test
    void registrationPublicAndStoresPlainPassword() throws Exception{
        mockMvc.perform(post("/api/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Anna",
                                    "lastName": "Verdi",
                                    "dateOfBirth": "1992-04-10",
                                    "username": "jwt.registration.user",
                                    "password": "passwanna"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
        Person saved = personRepository.findByUsername("jwt.registration.user")
                .orElseThrow();
        assertEquals("passwanna", saved.getPassword());
    }

    @Test
    void loginReturnsUsableToken() throws Exception{
        savePerson();
        String token = loginAndGetToken();
        mockMvc.perform(get("/api/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordReturnUnauthorized() throws Exception{
        savePerson();
        mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "username": "mario.rossi",
                                    "password": "WrongPassword!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unknownUsernameReturnsUnauthorized() throws Exception{
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "jwt.unknown.user",
                                    "password": "Password123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCredentialsReturnsBadRequest() throws Exception{
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN_DATA"));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception{
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception{
        mockMvc.perform(get("/api/account")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiresTokenReturnsUnauthorized() throws Exception{
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("contocorrente")
                .subject("jwt.test.user")
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(300))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception{
        savePerson();
        mockMvc.perform(post("/api/person")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Altro",
                                    "lastName": "Utente",
                                    "dateOfBirth": "1990-05-15",
                                    "username": "mario.rossi",
                                    "password": "Password123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }


}
