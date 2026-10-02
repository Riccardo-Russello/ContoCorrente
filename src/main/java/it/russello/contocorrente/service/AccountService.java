package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.AccountCreateRequest;
import it.russello.contocorrente.dto.AccountResponse;
import it.russello.contocorrente.dto.AccountUpdateRequest;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.AccountTransactionRepository;
import it.russello.contocorrente.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    public AccountService(AccountRepository accountRepository,
                          PersonRepository personRepository,
                          AccountTransactionRepository accountTransactionRepository) {
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.accountTransactionRepository = accountTransactionRepository;
    }

    public AccountResponse create(AccountCreateRequest request){
        if(request == null){
            throw invalidAccount("Corpo della richiesta obbligatorio");
        }
        if(request.personId() == null){
            throw invalidAccount("Id persona obbligatorio");
        }
        validateAccountNumber(request.accountNumber());
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "PERSON_NOT_FOUND",
                        "Persona non trovata"));

        if(accountRepository.existsByAccountNumber(request.accountNumber())){
            throw duplicateAccountNumber();
        }
        Account account = new Account(person, request.accountNumber());
        Account savedAccount = accountRepository.saveAndFlush(account);
        return AccountResponse.from(savedAccount);
    }

    public List<AccountResponse> findAll(){
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::from)
                .toList();
    }
    public AccountResponse findById(Long id){
        return AccountResponse.from(requireAccount(id));
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest request){
        Account account = requireAccount(id);
        if(request == null){
            throw invalidAccount("Corpo della richiesta obbligatorio");
        }
        validateAccountNumber(request.accountNumber());
        if(accountRepository.existsByAccountNumberAndIdNot(
                request.accountNumber(), id)){
            throw duplicateAccountNumber();
        }
        account.setAccountNumber(request.accountNumber());
        Account savedAccount = accountRepository.saveAndFlush(account);
        return AccountResponse.from(savedAccount);
    }

    @Transactional
    public void delete(Long id){
        Account account = requireAccount(id);
        if(account.getBalance().compareTo(BigDecimal.ZERO) != 0){
            throw new ApiException(HttpStatus.CONFLICT,
                    "ACCOUNT_BALANCE:NOT_ZERO",
                    "Conto può essere eliminato solo con saldo 0");
        }
        if(accountTransactionRepository.existsByAccount_Id(id)){
            throw new ApiException(HttpStatus.CONFLICT,
                    "ACCOUNT_HAS_TRANSACTION",
                    "Il conto ha movimenti associati e non può essere eliminato");
        }
        accountRepository.delete(account);
        accountRepository.flush();
    }

    private Account requireAccount(Long id){
        return accountRepository.findById(id)
                .orElseThrow( () ->new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ACCOUNT_NOT_FOUND",
                        "Conto non trovato"
                ));
    }
    private void validateAccountNumber(String accountNumber){
        if(accountNumber == null || accountNumber.isBlank()){
            throw invalidAccount("numero di account è obbligatorio, non può avere solo spazi");
        }
        if(accountNumber.length() > 30){
            throw invalidAccount("Numero di account non superiore a 30 caratteri");
        }
    }
    private ApiException invalidAccount(String message){
        return new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_ACCOUNT_DATA", message);
    }
    private ApiException duplicateAccountNumber(){
        return new ApiException(HttpStatus.CONFLICT,
                "ACCOUNT_NUMERB_ALREADY_EXISTS",
                "Numero di conto già utilizzato");
    }
}
