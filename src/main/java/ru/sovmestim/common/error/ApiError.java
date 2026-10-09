package ru.sovmestim.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Stable error payload returned to clients.
 *
 * @param code the machine-readable error code
 * @param message the human-readable error message
 * @param details per-field validation details
 * @param timestamp the moment the error was produced
 */
public record ApiError(String code, String message, List<String> details, Instant timestamp) {

    /**
     * Creates an error payload without validation details.
     *
     * @param code the machine-readable error code
     * @param message the human-readable error message
     * @return the error payload stamped with the current time
     */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), Instant.now());
    }

    /**
     * Creates an error payload with validation details.
     *
     * @param code the machine-readable error code
     * @param message the human-readable error message
     * @param details per-field validation details
     * @return the error payload stamped with the current time
     */
    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, details, Instant.now());
    }
}
