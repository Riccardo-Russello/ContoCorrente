package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.PersonRequest;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class PersonServiceTest {
    @Mock
    private PersonRepository personRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private PersonService personService;

    @Test
    void createPersonInvalidData(){
        PersonRequest request = new PersonRequest("Mario", "Rossi", LocalDate.now().plusDays(1));

        ApiException exception = assertThrows(ApiException.class, () -> personService.create(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("INVALID_PERSON_DATA", exception.getCode());

        verifyNoInteractions(personRepository, accountRepository);
    }
}
