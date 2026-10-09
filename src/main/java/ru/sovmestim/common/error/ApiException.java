package ru.sovmestim.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, mapped application errors.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Creates an exception mapped to the given HTTP status.
     *
     * @param status the HTTP status the error maps to
     * @param message the error message
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status this error maps to.
     *
     * @return the HTTP status
     */
    public HttpStatus status() {
        return status;
    }
}
