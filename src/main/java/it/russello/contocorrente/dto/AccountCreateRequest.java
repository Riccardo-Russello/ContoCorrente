package it.russello.contocorrente.dto;

public record AccountCreateRequest(Long personId,
                                   String accountNumber) {
}
