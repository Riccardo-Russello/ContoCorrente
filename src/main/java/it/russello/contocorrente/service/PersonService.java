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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @PreAuthorize("isAnonymous() or hasRole('admin')")
    public PersonResponse create(PersonRequest request) {
        if(personRepository.existsByUsername(request.username())) {
            throw  new ApiException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username già utilizzato");
        }

        Person person = new Person(request.firstName(), request.lastName(), request.dateOfBirth());
        person.setUsername(request.username());
        person.setPassword(passwordEncoder.encode(request.password()));

        Person savedPerson = personRepository.save(person);
        return PersonResponse.from(savedPerson);
    }

    @PreAuthorize("hasRole('admin')")
    public List<PersonResponse> findAll() {
        return personRepository.findAll()
                .stream()
                .map(PersonResponse::from)
                .toList();
    }

    @PreAuthorize("hasRole('admin')")
    public PersonResponse findById(Long id) {
        return PersonResponse.from(requirePerson(id));
    }

    @PreAuthorize("hasRole('admin')")
    public PersonResponse update(Long id, PersonRequest request) {
        Person person = requirePerson(id);
        person.setFirstName(request.firstName());
        person.setLastName(request.lastName());
        person.setDateOfBirth(request.dateOfBirth());

        Person savedPerson = personRepository.save(person);
        return PersonResponse.from(savedPerson);
    }

    @PreAuthorize("hasRole('admin')")
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
}
