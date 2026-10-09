package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Common view of a single sync change: a client-generated id, the client modification time and the
 * deletion flag. Every change record implements it so the apply flow can stay generic.
 */
public interface SyncChange {

    /**
     * Returns the client-generated id of the changed record.
     *
     * @return the record id
     */
    UUID id();

    /**
     * Returns the client modification time used for conflict detection.
     *
     * @return the client modification time
     */
    Instant updatedAt();

    /**
     * Tells whether the record is deleted on the client.
     *
     * @return {@code true} when the change is a deletion
     */
    boolean deleted();
}
