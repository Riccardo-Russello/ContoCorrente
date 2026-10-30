package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.PersonRequest;
import it.russello.contocorrente.dto.PersonResponse;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PersonServiceTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 15);
    @Mock
    private PersonRepository personRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private PersonService personService;

    private static Person person(Long id){
        Person person = new Person("Mario", "Rossi", BIRTH_DATE);
        ReflectionTestUtils.setField(person, "id", id);
        return person;
    }

    @Test
    void createValidPerson(){
        Person saved = person(1L);
        when(personRepository.save(any(Person.class))).thenReturn(saved);
        when(passwordEncoder.encode("passwmario")).thenReturn("passwmario");
        PersonResponse response = personService.create(validRequest());
        assertEquals(new PersonResponse(1L, "Mario", "Rossi", BIRTH_DATE), response);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(captor.capture());

        Person submitted = captor.getValue();
        assertNull(submitted.getId());
        assertEquals("Mario", submitted.getFirstName());
        assertEquals("Rossi", submitted.getLastName());
        assertEquals(BIRTH_DATE, submitted.getDateOfBirth());
        assertEquals("mario.rossi", submitted.getUsername());
        assertEquals("passwmario", submitted.getPassword());
        verify(personRepository).existsByUsername("mario.rossi");
        verify(passwordEncoder).encode("passwmario");
        verifyNoInteractions(accountRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidRequest")
    void createPersonInvalidData(PersonRequest request){
        ApiException exception = assertThrows(ApiException.class, () -> personService.create(request));

        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_PERSON_DATA");
        verifyNoInteractions(personRepository, accountRepository);
    }

    @Test
    void createWithExistingUsername(){
        when(personRepository.existsByUsername("mario.rossi")).thenReturn(true);
        ApiException exception = assertThrows(ApiException.class, () -> personService.create(validRequest()));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("USERNAME_ALREADY_EXISTS", exception.getCode());
        verify(personRepository, never()).save(any(Person.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createNames30CharAndBirthDateToday(){
        LocalDate today = LocalDate.now();
        PersonRequest request = new PersonRequest("a".repeat(30),"b".repeat(30), today, "more30.char", "passwmario");
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("passwmario")).thenReturn("passwmario");
        PersonResponse response = personService.create(request);

        assertEquals(request.firstName(), response.firstName());
        assertEquals(request.lastName(), response.lastName());
        assertEquals(today, response.dateOfBirth());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void findAll(){
        Person first = person(1L);
        Person second = new Person("Anna", "Bianchi", LocalDate.of(1985, 1, 10));
        ReflectionTestUtils.setField(second, "id", 2L);
        when(personRepository.findAll()).thenReturn(List.of(first, second));
        List<PersonResponse> responses = personService.findAll();

        assertEquals(List.of(PersonResponse.from(first), PersonResponse.from(second)), responses);
        verify(personRepository).findAll();
    }

    @Test
    void findByIdReturnsEmptyList(){
        when(personRepository.findAll()).thenReturn(List.of());
        assertTrue(personService.findAll().isEmpty());
        verify(personRepository).findAll();
    }

    @Test
    void findByIdReturnsPerson(){
        Person existing = person(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        PersonResponse response = personService.findById(1L);

        assertEquals(PersonResponse.from(existing), response);
        verify(personRepository).findById(1L);
    }

    @Test
    void findByIdMissingPerson(){
        when(personRepository.findById(1L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> personService.findById(1L));
        assertApiError(exception, HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND");

    }

    @Test
    void updateExistingPerson(){
        Person existing = person(1L);
        PersonRequest request = new PersonRequest("Luigi", "Verdi", LocalDate.of(1988, 3, 20), null, null);

        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(personRepository.save(existing)).thenReturn(existing);

        PersonResponse response = personService.update(1L, request);
        assertEquals(new PersonResponse(1L, "Luigi", "Verdi", LocalDate.of(1988, 3, 20)), response);

        assertEquals("Luigi", existing.getFirstName());
        assertEquals("Verdi", existing.getLastName());
        assertEquals(request.dateOfBirth(), existing.getDateOfBirth());
        verify(personRepository).save(existing);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void updateMissingPerson(){
        when(personRepository.findById(1L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> personService.update(1L, validRequest()));

        assertApiError(exception, HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND");
        verify(personRepository, never()).save(any(Person.class));
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void updateMustNotChangeCredentials(){
        Person existing = person(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        ApiException exception = assertThrows(ApiException.class, () -> personService.update(1L, validRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(personRepository, never()).save(any(Person.class));
    }

    @ParameterizedTest
    @MethodSource("invalidRequest")
    void updateInvalidDataNoChangingPerson(PersonRequest request){
        Person existing = person(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        ApiException exception = assertThrows(ApiException.class, () -> personService.update(1L, request));

        assertApiError(exception, HttpStatus.BAD_REQUEST, "INVALID_PERSON_DATA");
        assertEquals("Mario", existing.getFirstName());
        assertEquals("Rossi", existing.getLastName());
        assertEquals(BIRTH_DATE, existing.getDateOfBirth());
        verify(personRepository, never()).save(any(Person.class));
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void deletePersonWithNoAccount(){
        Person existing = person(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByPerson_Id(1L)).thenReturn(false);

        personService.delete(1L);

        var order = inOrder(personRepository, accountRepository);
        order.verify(personRepository).findById(1L);
        order.verify(accountRepository).existsByPerson_Id(1L);
        order.verify(personRepository).delete(existing);
        order.verify(personRepository).flush();
    }

    @Test
    void deletePersonWithAccount(){
        Person existing = person(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByPerson_Id(1L)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> personService.delete(1L));

        assertApiError(exception, HttpStatus.CONFLICT, "PERSON_HAS_ACCOUNT");
        verify(personRepository, never()).delete(any(Person.class));
        verify(personRepository, never()).flush();
    }

    @Test
    void deleteMissingPerson(){
        when(personRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> personService.delete(1L));

        assertApiError(exception, HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND");

        verifyNoMoreInteractions(accountRepository);
        verify(personRepository, never()).delete(any(Person.class));
        verify(personRepository, never()).flush();
    }

    private static PersonRequest validRequest(){
        return new PersonRequest("Mario", "Rossi", BIRTH_DATE, "mario.rossi", "passwmario");
    }
    private static PersonRequest validUpdateRequest(){
        return new PersonRequest("Mario", "Rossi", BIRTH_DATE, null, null);
    }
    private static void assertApiError(ApiException exception, HttpStatus status, String code){
        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
    }
    static Stream<PersonRequest> invalidRequest(){
        return Stream.of(
                (PersonRequest) null,
                new PersonRequest(null, "Rossi", BIRTH_DATE, null, null),
                new PersonRequest("", "Rossi", BIRTH_DATE, null, null),
                new PersonRequest("   ", "Rossi", BIRTH_DATE, null, null),
                new PersonRequest("a".repeat(31), "Rossi", BIRTH_DATE, null, null),
                new PersonRequest("Mario", null, BIRTH_DATE, null, null),
                new PersonRequest("Mario", "", BIRTH_DATE, null, null),
                new PersonRequest("Mario", "   ", BIRTH_DATE, null, null),
                new PersonRequest("Mario", "a".repeat(31), BIRTH_DATE, null, null),
                new PersonRequest("Mario", "Rossi", null, null, null),
                new PersonRequest("Mario", "Rossi", LocalDate.now().plusDays(1), null, null));
    }
}
