package ru.sovmestim.common.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the requested resource does not exist.
 */
public class NotFoundException extends ApiException {

    /**
     * Creates a 404 error with the given message.
     *
     * @param message the error message
     */
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
