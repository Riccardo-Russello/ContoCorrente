package it.russello.contocorrente.dto;

import it.russello.contocorrente.entity.Person;

import java.time.LocalDate;

public record PersonResponse(Long id, String first_name, String last_name, LocalDate date_of_birth) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(person.getId(),
                                    person.getFirstName(),
                                    person.getLastName(),
                                    person.getDateOfBirth());
    }   // Il controller restituisce questo DTO senza esporre le entity JPA
}
