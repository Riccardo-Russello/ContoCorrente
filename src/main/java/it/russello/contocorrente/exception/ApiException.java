package it.russello.contocorrente.exception;

import org.springframework.http.HttpStatus;

// Comunica dal Service un errore dando stato HTTP, codice e il messaggio
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
      return status;
    }
    public String getCode() {
      return code;
    }
}
