package it.russello.contocorrente.controller;

import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static it.russello.contocorrente.support.TestJwt.admin;
import static it.russello.contocorrente.support.TestJwt.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PersonRepository personRepository;
    @Autowired
    AccountRepository accountRepository;

    Person owner;
    Person other;
    Account source;
    Account destination;
    Account empty;

    @BeforeEach
    void setUp() {
        owner = savePerson("auth.owner");
        other = savePerson("auth.other");

        source = new Account(owner, "AUTH-SOURCE");
        source.setBalance(new BigDecimal("100.00"));
        accountRepository.saveAndFlush(source);

        destination = accountRepository.saveAndFlush(
                new Account(other, "AUTH-DESTINATION"));

        empty = accountRepository.saveAndFlush(
                new Account(owner, "AUTH-EMPTY"));
    }

    private Person savePerson(String username) {
        Person person = new Person(
                "Mario", "Rossi", LocalDate.of(1990, 5, 15));
        person.setUsername(username);
        person.setPassword("fixture-password");
        return personRepository.saveAndFlush(person);
    }

    private MockHttpServletRequestBuilder operation(String operation) {
        return switch (operation) {
            case "PERSON_LIST" -> get("/api/person");
            case "PERSON_READ" -> get("/api/person/{id}", owner.getId());
            case "PERSON_UPDATE" -> put("/api/person/{id}", owner.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"firstName":"Anna","lastName":"Verdi","dateOfBirth":"1990-05-15"}
                            """);
            case "PERSON_DELETE" -> delete("/api/person/{id}", other.getId());

            case "ACCOUNT_CREATE" -> post("/api/account")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"personId":%d,"accountNumber":"AUTH-NEW"}
                            """.formatted(owner.getId()));
            case "ACCOUNT_LIST" -> get("/api/account");
            case "ACCOUNT_READ" -> get("/api/account/{id}", source.getId());
            case "ACCOUNT_BALANCE" -> get("/api/account/{id}/balance", source.getId());
            case "ACCOUNT_UPDATE" -> put("/api/account/{id}", source.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountNumber\":\"AUTH-UPDATED\"}");
            case "ACCOUNT_DELETE" -> delete("/api/account/{id}", empty.getId());

            case "TRANSACTION_CREATE" -> post(
                    "/api/account/{id}/transaction", source.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":10,\"type\":\"DEPOSIT\"}");
            case "TRANSACTION_LIST" -> get(
                    "/api/account/{id}/transaction", source.getId());
            case "TRANSFER" -> post(
                    "/api/account/{id}/transaction/transfer", source.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"destinationAccountId":%d,"amount":10}
                            """.formatted(destination.getId()));
            default -> throw new IllegalArgumentException(operation);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PERSON_LIST", "PERSON_READ", "PERSON_UPDATE", "PERSON_DELETE",
            "ACCOUNT_CREATE", "ACCOUNT_LIST", "ACCOUNT_READ", "ACCOUNT_BALANCE",
            "ACCOUNT_UPDATE", "ACCOUNT_DELETE",
            "TRANSACTION_CREATE", "TRANSACTION_LIST", "TRANSFER"
    })
    void protectedOperationsRequireAuthentication(String operation) throws Exception {
        mockMvc.perform(operation(operation))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PERSON_LIST", "PERSON_READ", "PERSON_UPDATE", "PERSON_DELETE",
            "ACCOUNT_UPDATE", "ACCOUNT_DELETE"
    })
    void regularUserCannotPerformAdminOperations(String operation) throws Exception {
        mockMvc.perform(operation(operation).with(user(owner.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ACCOUNT_READ", "ACCOUNT_BALANCE",
            "TRANSACTION_CREATE", "TRANSACTION_LIST", "TRANSFER"
    })
    void userCannotOperateOnAnotherPersonsAccount(String operation) throws Exception {
        BigDecimal before = source.getBalance();

        mockMvc.perform(operation(operation).with(user(other.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        assertEquals(before,
                accountRepository.findById(source.getId()).orElseThrow().getBalance());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ACCOUNT_CREATE", "ACCOUNT_LIST", "ACCOUNT_READ", "ACCOUNT_BALANCE",
            "TRANSACTION_CREATE", "TRANSACTION_LIST", "TRANSFER"
    })
    void ownerCanPerformAllowedOperations(String operation) throws Exception {
        int expected = switch (operation) {
            case "ACCOUNT_CREATE", "TRANSACTION_CREATE", "TRANSFER" -> 201;
            default -> 200;
        };

        mockMvc.perform(operation(operation).with(user(owner.getId())))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PERSON_LIST", "PERSON_READ", "PERSON_UPDATE",
            "ACCOUNT_CREATE", "ACCOUNT_LIST", "ACCOUNT_READ", "ACCOUNT_BALANCE",
            "ACCOUNT_UPDATE", "ACCOUNT_DELETE",
            "TRANSACTION_CREATE", "TRANSACTION_LIST", "TRANSFER"
    })
    void adminCanPerformAllowedOperations(String operation) throws Exception {
        int expected = switch (operation) {
            case "ACCOUNT_CREATE", "TRANSACTION_CREATE", "TRANSFER" -> 201;
            case "ACCOUNT_DELETE" -> 204;
            default -> 200;
        };

        mockMvc.perform(operation(operation).with(admin(owner.getId())))
                .andExpect(status().is(expected));
    }

    @Test
    void userListContainsOnlyOwnAccounts() throws Exception {
        mockMvc.perform(get("/api/account").with(user(other.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(destination.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void adminListContainsAccountsOfBothPersons() throws Exception {
        mockMvc.perform(get("/api/account").with(admin(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void userCannotCreateAccountForAnotherPerson() throws Exception {
        long before = accountRepository.count();

        mockMvc.perform(post("/api/account")
                        .with(user(other.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personId":%d,"accountNumber":"FORBIDDEN"}
                                """.formatted(owner.getId())))
                .andExpect(status().isForbidden());

        assertEquals(before, accountRepository.count());
    }

    @Test
    void userCanCreateAccountWithoutExplicitPersonId() throws Exception {
        mockMvc.perform(post("/api/account")
                        .with(user(owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"IMPLICIT-OWNER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId").value(owner.getId()));
    }

    @Test
    void authenticatedUserCannotRegisterAnotherPerson() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(user(owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Anna",
                                  "lastName":"Verdi",
                                  "dateOfBirth":"1990-05-15",
                                  "username":"new.person",
                                  "password":"Password123!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingAccountReturns404ForAdmin() throws Exception {
        mockMvc.perform(get("/api/account/{id}", Long.MAX_VALUE)
                        .with(admin(owner.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void missingAccountReturns403ForUser() throws Exception {
        mockMvc.perform(get("/api/account/{id}", Long.MAX_VALUE)
                        .with(user(owner.getId())))
                .andExpect(status().isForbidden());
    }
}