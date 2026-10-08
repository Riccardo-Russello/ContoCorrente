package it.russello.contocorrente.dto;

import it.russello.contocorrente.entity.TransactionType;

import java.math.BigDecimal;

public record AccountTransactionRequest(BigDecimal amount,
                                        TransactionType type,
                                        Long to) {
    public record TransferRequest(Long destinationAccountId, BigDecimal amount) {}
}
