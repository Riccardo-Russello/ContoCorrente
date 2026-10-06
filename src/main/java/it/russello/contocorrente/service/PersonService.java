package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.PersonRequest;
import it.russello.contocorrente.dto.PersonResponse;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;

    public PersonResponse create(PersonRequest request) {
        validate(request);
        Person person = new Person(request.firstName(), request.lastName(), request.dateOfBirth());

        Person savedPerson = personRepository.save(person);
        return PersonResponse.from(savedPerson);
    }

    public List<PersonResponse> findAll() {
        return personRepository.findAll()
                .stream()
                .map(PersonResponse::from)
                .toList();
    }

    public PersonResponse findById(Long id) {
        return PersonResponse.from(requirePerson(id));
    }

    public PersonResponse update(Long id, PersonRequest request) {
        Person person = requirePerson(id);
        validate(request);

        person.setFirstName(request.firstName());
        person.setLastName(request.lastName());
        person.setDateOfBirth(request.dateOfBirth());

        Person savedPerson = personRepository.save(person);
        return PersonResponse.from(savedPerson);
    }

    public void delete(Long id) {
        Person person = requirePerson(id);
        if (accountRepository.existsByPerson_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "PERSON_HAS_ACCOUNT", "La persona ha conti associati");
        }

        personRepository.delete(person);
        personRepository.flush();       // Forza l'esecuzione della cancellazione prima dell'uscita dal metodo
    }


    private Person requirePerson(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND", "Persona non trovata"));
    }

    private void validate(PersonRequest request) {
        if (request == null) {
            throw invalidPerson("Corpo della richiesta obbligatorio");
        }
        validateName(request.firstName(), "first_name");
        validateName(request.lastName(), "last_name");

        if (request.dateOfBirth() == null) {
            throw invalidPerson("Data di nascita è obbligatoria");
        }
        if (request.dateOfBirth().isAfter(LocalDate.now())) {
            throw invalidPerson("La data di nascita non può essere futura");
        }
    }

    private void validateName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalidPerson(field + " è obbligatorio, non può avere solo spazi");
        }
        if (value.length() > 30) {
            throw invalidPerson(field + " non deve essere più di 30 caratteri");
        }
    }

    private ApiException invalidPerson(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PERSON_DATA", message);
    }
}
