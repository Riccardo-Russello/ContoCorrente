package it.russello.contocorrente.controller;

import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PersonControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void createPerson() throws Exception {
        long counterBefore = personRepository.count();
        var response = mockMvc.perform(post("/api/person")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "firstName" : "Mario",
                            "lastName" : "Rossi",
                            "dateOfBirth" : "1990-05-15"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"))
                .andReturn();
        entityManager.flush();
        entityManager.clear();
        assertEquals(counterBefore + 1, personRepository.count());

        Number responseId = com.jayway.jsonpath.JsonPath.read(response.getResponse().getContentAsString(), "$.id");

        Person saved = personRepository.findById(responseId.longValue()).orElseThrow();
        assertEquals(saved.getFirstName(), "Mario");
        assertEquals(saved.getLastName(), "Rossi");
        assertEquals(saved.getDateOfBirth(), LocalDate.of(1990, 5, 15));
    }

    @Test
    void findAllPerson() throws Exception{
        Person person = personRepository.saveAndFlush(new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15)));
        Long id = person.getId();
        mockMvc.perform(get("/api/person"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].firstName").value("Mario"))
                .andExpect(jsonPath("$[0].lastName").value("Rossi"))
                .andExpect(jsonPath("$[0].dateOfBirth").value("1990-05-15"));
    }

    @Test
    void findPersonById() throws Exception{
        Person person = personRepository.saveAndFlush(new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15)));
        Long id = person.getId();
        entityManager.clear();

        mockMvc.perform(get("/api/person/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"));
    }

    @Test
    void updatePerson() throws Exception{
        Person person = personRepository.saveAndFlush(new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15)));
        Long id = person.getId();
        entityManager.clear();
        long counterBefore = personRepository.count();

        mockMvc.perform(put("/api/person/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "firstName": "Anna",
                                    "lastName": "Verdi",
                                    "dateOfBirth": "1995-02-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.lastName").value("Verdi"))
                .andExpect(jsonPath("$.dateOfBirth").value("1995-02-10"));
        entityManager.flush();
        entityManager.clear();
        assertEquals(counterBefore, personRepository.count());

        Person updated = personRepository.findById(id).orElseThrow();
        assertEquals("Anna", updated.getFirstName());
        assertEquals("Verdi", updated.getLastName());
        assertEquals(LocalDate.of(1995, 2, 10), updated.getDateOfBirth());
    }

    @Test
    void deletePerson() throws Exception{
        Person person = personRepository.saveAndFlush(new Person("Mario", "Rossi", LocalDate.of(1990, 5, 15)));
        Long id = person.getId();
        entityManager.clear();
        long counterBefore = personRepository.count();

        mockMvc.perform(delete("/api/person/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();

        assertFalse(personRepository.existsById(id));
        assertEquals(counterBefore -1 , personRepository.count());
    }
}
