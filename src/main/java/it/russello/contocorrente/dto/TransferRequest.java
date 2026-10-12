package it.russello.contocorrente.dto;

import java.math.BigDecimal;

public record TransferRequest(Long destinationAccountId, BigDecimal amount){}
