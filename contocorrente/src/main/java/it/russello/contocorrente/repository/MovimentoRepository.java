package it.russello.contocorrente.repository;

import it.russello.contocorrente.entity.Movimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentoRepository extends JpaRepository<Movimento, Long> {

    // Filtra i movimenti per conto.id, li ordina per data più recente, usa l'id decrescente per data uguale & restituisce massimo 5 elementi
    List<Movimento> findTop5ByConto_IdOrderByDataTransazioneDescIdDesc(Long idConto);
}
