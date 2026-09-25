package it.russello.contocorrente;

import it.russello.contocorrente.entity.Conto;
import it.russello.contocorrente.repository.ContoRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InizializzazioneConto implements CommandLineRunner {
    private final ContoRepository contoRepository;

    public InizializzazioneConto(ContoRepository contoRepository) {
        this.contoRepository = contoRepository;
    }

    @Override
    @Transactional
    public void run(String... args){
        String numeroConto = "CC000001";
        if(!contoRepository.existsByNumeroConto(numeroConto)){
            contoRepository.save(new Conto(numeroConto));
        }
    }
}
