package cl.ipt.siga.exception;

import org.springframework.http.HttpStatus;


public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    // Constructor por defecto: asume 400 Bad Request
    public BusinessRuleException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    // Constructor sobrecargado: permite pasar HttpStatus.CONFLICT (409)
    public BusinessRuleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}