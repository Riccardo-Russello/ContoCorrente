package it.russello.contocorrente.controller;

import com.jayway.jsonpath.JsonPath;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AccountControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private EntityManager entityManager;

    private Person person;
    @BeforeEach
    void setUp(){
        person = new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15));
        person.setUsername("mario.rossi");
        person.setPassword("passwmario");
        personRepository.saveAndFlush(person);
    }

    @Test
    void createAccount() throws Exception {
        long counterBefore = accountRepository.count();
        var response = mockMvc.perform(post("/api/account").with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "personId" : %d,
                                "accountNumber" : "ACC-001"
                            }
                            """.formatted(person.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.personId").value(person.getId()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-001"))
                .andExpect(jsonPath("$.openedAt").isNotEmpty())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andReturn();
        entityManager.flush();
        entityManager.clear();

        Number id = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        Account saved = accountRepository.findById(id.longValue()).orElseThrow();
        assertEquals(counterBefore + 1, accountRepository.count());
        assertEquals(person.getId(), saved.getPerson().getId());
        assertEquals("ACC-001", saved.getAccountNumber());
        assertNotNull(saved.getOpenedAt());
        assertEquals(0, saved.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void findAllAccounts()  throws Exception {
        Account account = accountRepository.saveAndFlush(new Account(person, "ACC-001"));
        entityManager.clear();
        mockMvc.perform(get("/api/account").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(account.getId()))
                .andExpect(jsonPath("$[0].personId").value(person.getId()))
                .andExpect(jsonPath("$[0].accountNumber").value("ACC-001"))
                .andExpect(jsonPath("$[0].openedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].balance").value(0.0));
    }

    @Test
    void findAccountById() throws Exception {
        Account account = accountRepository.saveAndFlush(new Account(person, "ACC-001"));
        entityManager.clear();
        mockMvc.perform(get("/api/account/{id}", account.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId()))
                .andExpect(jsonPath("$.personId").value(person.getId()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-001"))
                .andExpect(jsonPath("$.openedAt").isNotEmpty())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    @Test
    void getAccountBalance() throws Exception {
        Account account = new Account(person, "ACC-001");
        account.setBalance(new BigDecimal("125.50"));
        accountRepository.saveAndFlush(account);
        entityManager.clear();
        mockMvc.perform(get("/api/account/{id}/balance", account.getId()).with(jwt()))
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.balance").value(125.50))
                .andExpect(jsonPath("$.currency").value("€ (EUR)"));
    }

    @Test
    void updateAccount() throws Exception{
        Account account = accountRepository.saveAndFlush(new Account(person, "ACC-001"));
        Long id = account.getId();
        long counterBefore = accountRepository.count();
        entityManager.clear();
        mockMvc.perform(put("/api/account/{id}", id).with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                    "accountNumber": "ACC-002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.personId").value(person.getId()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-002"))
                .andExpect(jsonPath("$.balance").value(0.0));
        entityManager.flush();
        entityManager.clear();
        Account updated = accountRepository.findById(id).orElseThrow();
        assertEquals(counterBefore, accountRepository.count());
        assertEquals("ACC-002", updated.getAccountNumber());
        assertEquals(person.getId(), updated.getPerson().getId());
        assertEquals(account.getOpenedAt(), updated.getOpenedAt());
        assertEquals(0, updated.getBalance().compareTo(account.getBalance()));
    }

    @Test
    void deleteAccount() throws Exception {
        Account account = accountRepository.saveAndFlush(new Account(person, "ACC-001"));
        Long id = account.getId();
        long counterBefore = accountRepository.count();
        entityManager.clear();
        mockMvc.perform(delete("/api/account/{id}", id).with(jwt()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        assertFalse(accountRepository.existsById(id));
        assertEquals(counterBefore -1 , accountRepository.count() );
        assertTrue(personRepository.existsById(person.getId()));
    }
}
