package it.russello.contocorrente.config;

import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/* Spring esegue il CommandLineRunner durante l'avvio. Serve a controllare
    a ogni avvio del backend che il conto esiste e crearlo solo se manca. */
@Component
public class InizializzazioneConto implements CommandLineRunner {
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;

    public InizializzazioneConto(AccountRepository accountRepository,
                                 PersonRepository personRepository) {
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
    }

    @Override
    @Transactional
    public void run(String... args){                                // Metodo chiamato sui componenti che implementano l'interfaccia
        String accountNumber = "CC000001";                            // Assumo come primo numeroConto
        if(accountRepository.existsByAccountNumber(accountNumber)){      // Se il conto con questo numero esiste, esce
            return;
        }
        Person person = new Person("Mario", "Rossi",    //.. altrimenti crea la persona, l'account e li salva entrambi
                LocalDate.of(1990, 5, 15));
        Person savedPerson = personRepository.save(person);
        Account account = new Account(savedPerson, accountNumber);
        accountRepository.save(account);

    }
}
