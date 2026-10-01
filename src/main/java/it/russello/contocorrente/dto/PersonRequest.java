package it.russello.contocorrente.dto;

import java.time.LocalDate;

public record PersonRequest(String first_name, String last_name,
                            LocalDate date_of_birth) {
}
