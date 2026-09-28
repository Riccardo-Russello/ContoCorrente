package it.russello.contocorrente.config;

import it.russello.contocorrente.entity.Conto;
import it.russello.contocorrente.repository.ContoRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/* Spring esegue il CommandLineRunner durante l'avvio. Serve a controllare
    a ogni avvio del backend che il conto esiste e crearlo solo se manca. */
@Component
public class InizializzazioneConto implements CommandLineRunner {
    private final ContoRepository contoRepository;

    public InizializzazioneConto(ContoRepository contoRepository) {
        this.contoRepository = contoRepository;
    }

    @Override
    @Transactional
    public void run(String... args){                                // Metodo chiamato sui componenti che implementano l'interfaccia
        String numeroConto = "CC000001";                            // Assumo come primo numeroConto
        if(!contoRepository.existsByNumeroConto(numeroConto)){      // Se il conto con questo numero esiste lo crea e lo salva
            contoRepository.save(new Conto(numeroConto));
        }
    }
}
