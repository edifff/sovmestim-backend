package ru.sovmestim.common.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request conflicts with the current state of the resource.
 */
public class ConflictException extends ApiException {

    /**
     * Creates a 409 error with the given message.
     *
     * @param message the error message
     */
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
