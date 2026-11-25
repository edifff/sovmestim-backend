package ru.sovmestim.common.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request is malformed or semantically invalid.
 */
public class BadRequestException extends ApiException {

    /**
     * Creates a 400 error with the given message.
     *
     * @param message the error message
     */
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
