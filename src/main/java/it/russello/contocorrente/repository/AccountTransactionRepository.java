package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction>
    findTop5ByAccount_idOrderByOccurredAtDescIdDesc(Long idConto);

    boolean existsByAccount_Id(Long accountId);

}
