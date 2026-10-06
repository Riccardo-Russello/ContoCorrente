package it.russello.contocorrente.service;

import it.russello.contocorrente.dto.AccountTransactionRequest;
import it.russello.contocorrente.dto.AccountTransactionResponse;
import it.russello.contocorrente.dto.TransactionListResponse;
import it.russello.contocorrente.entity.Account;
import it.russello.contocorrente.entity.AccountTransaction;
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
