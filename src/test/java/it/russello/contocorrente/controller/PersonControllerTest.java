package it.russello.contocorrente.controller;

import it.russello.contocorrente.dto.PersonResponse;
import it.russello.contocorrente.service.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PersonController.class)
public class PersonControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PersonService personService;

    @Test
    void findById() throws Exception {
        PersonResponse response = new PersonResponse(1L, "Mario", "Rossi", LocalDate.of(1990, 5, 15));
        when(personService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/person/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.lastName").value("Rossi"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"));
        verify(personService).findById(1L);
    }

}
