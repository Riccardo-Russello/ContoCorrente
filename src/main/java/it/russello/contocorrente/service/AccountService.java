package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.AccountBalanceResponse;
import it.russello.contocorrente.dto.AccountCreateRequest;
import it.russello.contocorrente.dto.AccountResponse;
import it.russello.contocorrente.dto.AccountUpdateRequest;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.Person;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.AccountTransactionRepository;
import it.russello.contocorrente.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import it.russello.contocorrente.security.AccountSecurity;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AccountService {
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final AccountSecurity accountSecurity;

    @PreAuthorize("hasAnyRole('admin', 'user')")
    public AccountResponse create(AccountCreateRequest request) {
        Long currentPersonId = accountSecurity.currentPersonId();
        Long ownerPersonId = request.personId() == null ? currentPersonId : request.personId();
        if(!accountSecurity.isAdmin() && !currentPersonId.equals(ownerPersonId)){
            throw new AccessDeniedException("Puoi creare un conto soltanto per te stesso");
        }
        Person person = personRepository.findById(ownerPersonId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "PERSON_NOT_FOUND",
                        "Persona non trovata"));

        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw duplicateAccountNumber();
        }
        Account account = new Account(person, request.accountNumber());
        Account savedAccount = accountRepository.saveAndFlush(account);
        return AccountResponse.from(savedAccount);
    }

    @PreAuthorize("hasAnyRole('admin', 'user')")
    public List<AccountResponse> findAll() {
        List<Account> accounts;
        if(accountSecurity.isAdmin()){
            accounts = accountRepository.findAll();
        }else{
            Long personId = accountSecurity.currentPersonId();
            accounts = accountRepository.findByPerson_Id(personId);
        }
        return accounts.stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PreAuthorize("""
            hasRole('admin') or
            (hasRole('user') and @accountSecurity.isOwner(#id, authentication))
            """)
    public AccountResponse findById(Long id) {
        return AccountResponse.from(requireAccount(id));
    }

    @PreAuthorize("""
            hasRole('admin') or (hasRole('user') and @accountSecurity.isOwner(#id, authentication))
            """)
    public AccountBalanceResponse getBalance(@P("id")Long id){
        Account account = requireAccount(id);
        return AccountBalanceResponse.from(account);
    }


    public AccountResponse update(Long id, AccountUpdateRequest request) {
        Account account = requireAccount(id);
        if (accountRepository.existsByAccountNumberAndIdNot(
                request.accountNumber(), id)) {
            throw duplicateAccountNumber();
        }
        account.setAccountNumber(request.accountNumber());
        Account savedAccount = accountRepository.saveAndFlush(account);
        return AccountResponse.from(savedAccount);
    }


    public void delete(Long id) {
        Account account = requireAccount(id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "ACCOUNT_BALANCE_NOT_ZERO",
                    "Conto può essere eliminato solo con saldo 0");
        }
        if (accountTransactionRepository.existsByAccount_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "ACCOUNT_HAS_TRANSACTION",
                    "Il conto ha movimenti associati e non può essere eliminato");
        }
        accountRepository.delete(account);
        accountRepository.flush();
    }


    private Account requireAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ACCOUNT_NOT_FOUND",
                        "Conto non trovato"
                ));
    }
    private ApiException duplicateAccountNumber() {
        return new ApiException(HttpStatus.CONFLICT,
                "ACCOUNT_NUMBER_ALREADY_EXISTS",
                "Numero di conto già utilizzato");
    }

}
