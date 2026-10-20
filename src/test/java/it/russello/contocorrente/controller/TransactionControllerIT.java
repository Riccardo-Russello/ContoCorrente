package it.russello.contocorrente.controller;

import com.jayway.jsonpath.JsonPath;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.AccountTransaction;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.entity.TransactionType;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.AccountTransactionRepository;
import it.russello.contocorrente.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TransactionControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountTransactionRepository transactionRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private EntityManager entityManager;

    private Account source;
    private Account destination;

    @BeforeEach
    void setUp(){
        Person person = personRepository.saveAndFlush(
                new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15)));
        source = new Account(person, "ACC-001");
        source.setBalance(new BigDecimal("100.00"));
        accountRepository.saveAndFlush(source);
        destination = new Account(person, "ACC-002");
        destination.setBalance(new BigDecimal("50.00"));
        accountRepository.saveAndFlush(destination);
        entityManager.clear();
    }

    @ParameterizedTest
    @CsvSource({"DEPOSIT, 125.00", "WITHDRAWAL, 75.00"})
    void createTransaction(String type, BigDecimal expectedBalance) throws Exception {
        long counterBefore = transactionRepository.count();
        var response = mockMvc.perform(post("/api/account/{id}/transaction", source.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "amount": 25.00,
                                            "type": "%s"
                                        }
                                        """.formatted(type)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.sourceAccountId").value(source.getId()))
                .andExpect(jsonPath("$.balance").value(expectedBalance.doubleValue()))
                .andExpect(jsonPath("$.destinationAcccountId").doesNotExist())
                .andExpect(jsonPath("$.destinationBalance").doesNotExist())
                .andReturn();
        entityManager.flush();
        entityManager.clear();
        Number id = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        AccountTransaction saved = transactionRepository.findById(id.longValue()).orElseThrow();

        assertEquals(counterBefore + 1, transactionRepository.count());
        assertEquals(TransactionType.valueOf(type), saved.getType());
        assertEquals(0, new BigDecimal("25.00").compareTo(saved.getAmount()));
        assertEquals(source.getId(), saved.getAccount().getId());
        assertNull(saved.getDestinationAccount());
        assertEquals(0, expectedBalance.compareTo(
                accountRepository.findById(source.getId()).orElseThrow().getBalance()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void transfer(boolean dedicatedEndpoint)  throws Exception {
        long counterBefore = transactionRepository.count();
        String url = "/api/account/{id}/transaction" + (dedicatedEndpoint ? "/transfer" : "");
        String json = dedicatedEndpoint ? """
                  {
                      "destinationAccountId": %d,
                      "amount": 25.00
                  }
                  """.formatted(destination.getId())
                : """
                  {
                      "type": "TRANSFER",
                      "to": %d,
                      "amount": 25.00
                  }
                  """.formatted(destination.getId());
        var response = mockMvc.perform(post(url, source.getId())
                                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.sourceAccountId").value(source.getId()))
                .andExpect(jsonPath("$.destinationAccountId").value(destination.getId()))
                .andExpect(jsonPath("$.balance").value(75.00))
                .andExpect(jsonPath("$.destinationBalance").value(75.00))
                .andReturn();
        entityManager.flush();
        entityManager.clear();

        Number id = JsonPath.read(response.getResponse().getContentAsString(), "$.id");
        AccountTransaction saved = transactionRepository.findById(id.longValue()).orElseThrow();

        assertEquals(counterBefore + 1, transactionRepository.count());
        assertEquals(TransactionType.TRANSFER, saved.getType());
        assertEquals(0, new BigDecimal("25.00").compareTo(saved.getAmount()));
        assertEquals(source.getId(), saved.getAccount().getId());
        assertEquals(destination.getId(), saved.getDestinationAccount().getId());
        assertNotNull(saved.getOccurredAt());
        assertEquals(0, new BigDecimal("75.00").compareTo(
                accountRepository.findById(source.getId()).orElseThrow().getBalance()));
        assertEquals(0, new BigDecimal("75.00").compareTo(
                accountRepository.findById(destination.getId()).orElseThrow().getBalance()));
    }

    @Test
    void getLastFiveTransactions() throws Exception{
        var transactions = new ArrayList<AccountTransaction>();
        for(int i=1; i<=6; i++){
            transactions.add(transactionRepository.saveAndFlush(
                    new AccountTransaction(source, TransactionType.DEPOSIT, BigDecimal.valueOf(i))));
        }
        transactions.add(transactionRepository.saveAndFlush(
                new AccountTransaction(destination, source, new BigDecimal("10.00"))));
        transactionRepository.saveAndFlush(
                new AccountTransaction(destination, TransactionType.DEPOSIT, new  BigDecimal("99.00")));
        transactions.sort(Comparator.comparing(AccountTransaction::getOccurredAt)
                        .thenComparing(AccountTransaction::getId)
                        .reversed());
        entityManager.clear();
        var result = mockMvc.perform(get("/api/account/{id}/transaction", source.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        for(int i=0; i<5; i++){
            AccountTransaction expected = transactions.get(i);
            String path = "$[" + i + "]";
            result.andExpect(jsonPath(path + ".id").value(expected.getId()))
                    .andExpect(jsonPath(path + ".type").value(expected.getType().name()))
                    .andExpect(jsonPath(path + ".amount")
                            .value(expected.getAmount().doubleValue()))
                    .andExpect(jsonPath(path + ".occurredAt").isNotEmpty())
                    .andExpect(jsonPath(path + ".sourceAccountId")
                            .value(expected.getAccount().getId()));
            if (expected.getDestinationAccount() == null) {
                result.andExpect(jsonPath(path + ".destinationAccountId").doesNotExist());
            } else {
                result.andExpect(jsonPath(path + ".destinationAccountId")
                        .value(expected.getDestinationAccount().getId()));
            }
        }
    }

    @Test
    void getLastFiveTransactionsEmpty() throws Exception{
        mockMvc.perform(get("/api/account/{id}/transaction", source.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
