package it.russello.contocorrente.config;

import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.PersonRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

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
    public void run(String... args) {
        String accountNumber = "CC000001";
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            return;
        }
        Person person = new Person("Mario", "Rossi",
                LocalDate.of(1990, 5, 15));
        Person savedPerson = personRepository.save(person);
        Account account = new Account(savedPerson, accountNumber);
        accountRepository.save(account);

    }
}
