package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    // Filtra i movimenti per conto.id, li ordina per data più recente, usa l'id decrescente per data uguale & restituisce massimo 5 elementi
    List<AccountTransaction>
    findTop5ByAccount_idOrderByOccurredAtDescIdDesc(Long idConto);

    // Impedisce l'eliminazione di un conto con movimenti
    boolean existsByAccount_Id(Long accountId);

}
