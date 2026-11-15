package ru.sovmestim.common.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * Outcome of one synchronized write, reported by an owning service to the gateway.
 *
 * @param id the record id
 * @param status {@code APPLIED}, {@code CONFLICT} or {@code REJECTED}
 * @param message human-readable detail, or {@code null}
 * @param updatedAt server modification time, or {@code null}
 */
public record ChangeResult(UUID id, String status, String message, Instant updatedAt) {

    /** Status of a change that was accepted. */
    public static final String APPLIED = "APPLIED";

    /** Status of a change that lost to a newer server row. */
    public static final String CONFLICT = "CONFLICT";

    /** Status of a change that was not applied. */
    public static final String REJECTED = "REJECTED";

    private static final String CONFLICT_MESSAGE = "server row is newer";

    /**
     * Creates an applied outcome.
     *
     * @param id the record id
     * @param updatedAt server modification time
     * @return the outcome
     */
    public static ChangeResult applied(UUID id, Instant updatedAt) {
        return new ChangeResult(id, APPLIED, null, updatedAt);
    }

    /**
     * Creates a conflict outcome.
     *
     * @param id the record id
     * @return the outcome
     */
    public static ChangeResult conflict(UUID id) {
        return new ChangeResult(id, CONFLICT, CONFLICT_MESSAGE, null);
    }

    /**
     * Creates a rejected outcome.
     *
     * @param id the record id, may be {@code null}
     * @param message human-readable reason
     * @return the outcome
     */
    public static ChangeResult rejected(UUID id, String message) {
        return new ChangeResult(id, REJECTED, message, null);
    }
}
