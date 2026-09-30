package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);                    // Controlla se un numero è già assegnato
    Optional<Account> findByAccountNumber(String accountNumber);            // Recupera un conto dal suo numero
    boolean existsByAccountNumberAndIdNot(String accountNumber,Long id);    // Verifica l'unicità durante una modifica, escludendo il conto stesso
    boolean existsByPerson_Id(Long personId);                               // Verifica se una persona possiede conti prima del delete
    List<Account> findByPerson_Id(Long personId);                           // Recupera tutti i conti della persona
}
