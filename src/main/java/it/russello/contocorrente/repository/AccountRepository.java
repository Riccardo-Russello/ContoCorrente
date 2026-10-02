package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);

    boolean existsByPerson_Id(Long personId);

    List<Account> findByPerson_Id(Long personId);
}
