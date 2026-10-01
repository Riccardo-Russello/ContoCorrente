package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository  extends JpaRepository<Person,Long> {
    // Non servono altri metodi CRUD.
    // Metodi save, findById, findAll, existsById, delete già presenti
}
