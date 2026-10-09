package ru.sovmestim.common.persistence;

import java.time.Instant;
import java.util.UUID;

/**
 * A mutable entity that takes part in client-server sync: it exposes a client-visible id, an update
 * timestamp and the {@code deleted}/{@code synced} flags used by the sync protocol.
 *
 * <p>Soft deletion is expressed through {@link #markDeleted()} for a change made on this server and
 * still to be pulled by the client, and {@link #markDeletedSynced()} for a deletion that is already
 * reflected on both sides.
 */
public interface SyncedEntity {

    /**
     * Returns the client-assigned entity id.
     *
     * @return the entity id
     */
    UUID getId();

    /**
     * Returns the last time the entity was changed.
     *
     * @return the update timestamp
     */
    Instant getUpdatedAt();

    /**
     * Tells whether the entity is soft-deleted.
     *
     * @return {@code true} when the entity is a tombstone
     */
    boolean isDeleted();

    /**
     * Tells whether the entity is in sync with the client.
     *
     * @return {@code true} when the entity is synced
     */
    boolean isSynced();

    /**
     * Sets the soft-deletion flag.
     *
     * @param deleted the new flag value
     */
    void setDeleted(boolean deleted);

    /**
     * Sets the sync flag.
     *
     * @param synced the new flag value
     */
    void setSynced(boolean synced);

    /**
     * Marks the entity as deleted locally, waiting to be synchronized.
     */
    default void markDeleted() {
        setDeleted(true);
        setSynced(false);
    }

    /**
     * Marks the entity as deleted and already synchronized.
     */
    default void markDeletedSynced() {
        setDeleted(true);
        setSynced(true);
    }
}
