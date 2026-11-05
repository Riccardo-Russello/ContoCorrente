package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);

    boolean existsByPerson_Id(Long personId);

    Page<Account> findByPerson_Id(Long personId, Pageable pageable);

    boolean existsByIdAndPerson_Id(Long accountId, Long personId);
}
