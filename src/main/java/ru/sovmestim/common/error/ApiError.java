package ru.sovmestim.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Stable error payload returned to clients.
 */
public record ApiError(String code, String message, List<String> details, Instant timestamp) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), Instant.now());
    }

    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, details, Instant.now());
    }
}
