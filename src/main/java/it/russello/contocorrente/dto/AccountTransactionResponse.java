package it.russello.contocorrente.dto;

import it.russello.contocorrente.entity.AccountTransaction;
import it.russello.contocorrente.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountTransactionResponse(Long id, TransactionType type,
                                         BigDecimal amount, LocalDateTime occurredAt,
                                         BigDecimal balance) {
    public static AccountTransactionResponse from(AccountTransaction transaction,
                                                  BigDecimal balance) {
        return new AccountTransactionResponse(transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getOccurredAt(),
                balance);
    }
}
