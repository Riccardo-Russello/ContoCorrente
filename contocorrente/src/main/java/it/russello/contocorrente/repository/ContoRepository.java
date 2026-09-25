package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.Conto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContoRepository extends JpaRepository<Conto, Long> {
    boolean existsByNumeroConto(String numeroConto);
}
