package it.russello.contocorrente.dto;

import java.time.LocalDate;

public record PersonRequest(String firstName, String lastName,
                            LocalDate dateOfBirth, String username, String password) {
}
