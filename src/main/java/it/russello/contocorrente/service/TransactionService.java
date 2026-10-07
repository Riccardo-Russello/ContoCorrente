package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.AccountTransactionRequest;
import it.russello.contocorrente.dto.AccountTransactionRequest.TransferRequest;
import it.russello.contocorrente.dto.AccountTransactionResponse.TransferResponse;
import it.russello.contocorrente.dto.AccountTransactionResponse;
import it.russello.contocorrente.dto.TransactionListResponse;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.AccountTransaction;
import it.russello.contocorrente.entity.TransactionType;
import it.russello.contocorrente.exception.ApiException;
import it.russello.contocorrente.repository.AccountRepository;
import it.russello.contocorrente.repository.AccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999.99");

    @Transactional
    public AccountTransactionResponse createTransaction(Long accountId, AccountTransactionRequest request) {     // createAccountTransaction (spostare nel service separato)
        Account account = requireAccount(accountId);
        if (request == null) {
            throw invalidTransaction("Corpo della richiesta è obbligatorio");
        }
        if (request.type() == null) {
            throw invalidTransaction("Tipo obbligatorio: DEPOSIT o WITHDRAWAL");
        }
        BigDecimal amount = validateAmount(request.amount());
        BigDecimal newBalance;
        switch (request.type()) {
            case DEPOSIT -> {
                newBalance = account.getBalance().add(amount);
                if (newBalance.compareTo(MAX_MONEY) > 0) {
                    throw new ApiException(HttpStatus.CONFLICT, "BALANCE_LIMIT_EXCEEDED", "Versamento supera la capacità del saldo");
                }
            }
            case WITHDRAWAL -> {
                if (account.getBalance().compareTo(amount) < 0) {
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

    @Transactional
    public TransferResponse transfer(Long sourceAccountId, TransferRequest request) {
        if(request == null){
            throw invalidTransaction("Corpo della richiesta obbligatorio");
        }
        Long destinationAccountId = request.destinationAccountId();
        if(sourceAccountId == null || sourceAccountId <= 0 || destinationAccountId == null || destinationAccountId <= 0){
            throw invalidTransaction("Id dei conti devono essere positivi");
        }
        BigDecimal amount = validateAmount(request.amount());
        Account source = requireAccount(sourceAccountId);
        Account destination = requireAccount(destinationAccountId);

        if(source.getBalance().compareTo(amount) < 0){
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "Saldo insufficiente al trasferimento");
        }
         BigDecimal sourceBalance =  source.getBalance().subtract(amount);
        BigDecimal destinationBalance = destination.getBalance().add(amount);

        if(destinationBalance.compareTo(MAX_MONEY) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "BALANCE_LIMIT:EXCEEDED", "Il trasferimento supera la capacità del saldo");
        }
        source.setBalance(sourceBalance);
        destination.setBalance(destinationBalance);

        AccountTransaction withdrawal = new AccountTransaction(source, TransactionType.WITHDRAWAL, amount);
        AccountTransaction deposit = new AccountTransaction(destination, TransactionType.DEPOSIT, amount);
        AccountTransaction savedWithdrawal = accountTransactionRepository.save(withdrawal);
        AccountTransaction savedDeposit = accountTransactionRepository.save(deposit);

        accountTransactionRepository.flush();
        return new TransferResponse(source.getId(), destination.getId(),
                AccountTransactionResponse.from(savedWithdrawal, sourceBalance),
                AccountTransactionResponse.from(savedDeposit, destinationBalance));

    }

    public List<TransactionListResponse> getLastFiveTransactions(Long accountId) {
        requireAccount(accountId);
        return accountTransactionRepository
                .findTop5ByAccount_idOrderByOccurredAtDescIdDesc(accountId)
                .stream()
                .map(TransactionListResponse::from)
                .toList();
    }

    private Account requireAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ACCOUNT_NOT_FOUND",
                        "Conto non trovato"
                ));
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw invalidTransaction("Importo obbligatorio");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidTransaction("Importo deve essere maggiore di zero");
        }
        if (amount.scale() > 2) {
            throw invalidTransaction("Importo deve avere massimo due cifre decimali");
        }
        if (amount.compareTo(MAX_MONEY) > 0) {
            throw invalidTransaction("Limite di importo superato");
        }
        return amount.setScale(2);
    }

    private ApiException invalidTransaction(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_TRANSACTION_DATA", message);
    }
}
