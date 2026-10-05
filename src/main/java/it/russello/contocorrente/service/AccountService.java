package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.*;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.AccountTransaction;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.AccountTransactionRepository;
import it.russello.contocorrente.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999.99");

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


    public void delete(Long id){
        Account account = requireAccount(id);
        if(account.getBalance().compareTo(BigDecimal.ZERO) != 0){
            throw new ApiException(HttpStatus.CONFLICT,
                    "ACCOUNT_BALANCE_NOT_ZERO",
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

    public AccountTransactionResponse createTransaction(Long accountId, AccountTransactionRequest request){
        Account account = requireAccount(accountId);
        if(request == null){
            throw invalidTransaction("Corpo della richiesta è obbligatorio");
        }
        if(request.type() == null){
            throw invalidTransaction("Tipo obbligatorio: DEPOSIT o WITHDRAWAL");
        }
        BigDecimal amount = validateAmount(request.amount());
        BigDecimal newBalance;
        switch(request.type()){
            case DEPOSIT -> {
                newBalance = account.getBalance().add(amount);
                if(newBalance.compareTo(MAX_MONEY) > 0){
                    throw new ApiException(HttpStatus.CONFLICT, "BALANCE_LIMIT_EXCEEDED", "Versamento supera la capacità del saldo");
                }
            }
            case WITHDRAWAL -> {
                if(account.getBalance().compareTo(amount) < 0){
                    throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "Saldo insufficiente");
                }
                newBalance = account.getBalance().subtract(amount);
            }
            default -> throw invalidTransaction("Movimento non valido");
        }
        account.setBalance(newBalance);
        Account savedAccount = accountRepository.saveAndFlush(account);
        AccountTransaction transaction = new AccountTransaction(savedAccount, request.type(), amount);
        AccountTransaction savedTransaction = accountTransactionRepository.saveAndFlush(transaction);
        return AccountTransactionResponse.from(savedTransaction, savedAccount.getBalance());
    }

    private Account requireAccount(Long id){
        return accountRepository.findById(id)
                .orElseThrow( () ->new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ACCOUNT_NOT_FOUND",
                        "Conto non trovato"
                ));
    }
    private BigDecimal validateAmount(BigDecimal amount){
        if(amount == null){
            throw invalidTransaction("Importo obbligatorio");
        }
        if(amount.compareTo(BigDecimal.ZERO) <= 0){
            throw invalidTransaction("Importo deve essere maggiore di zero");
        }
        if(amount.scale() > 2){
            throw invalidTransaction("Importo deve avere massimo due cifre decimali");
        }
        if(amount.compareTo(MAX_MONEY) > 0){
            throw invalidTransaction("Limite di importo superato");
        }
        return amount.setScale(2);
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
                "ACCOUNT_NUMBER_ALREADY_EXISTS",
                "Numero di conto già utilizzato");
    }
    private ApiException invalidTransaction(String message){
        return new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_TRANSACTION_DATA", message);
    }
}
