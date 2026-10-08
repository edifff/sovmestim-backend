package ru.sovmestim.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, mapped application errors.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
