package ru.sovmestim.common.persistence;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Base class for the mutable, synchronized entities. Holds the audit and sync columns that are
 * identical for every such entity and keeps {@code updatedAt} current on persist and update.
 *
 * <p>The {@code id} is intentionally not declared here: some entities use a client-assigned UUID
 * (for sync idempotency) while others let the identifier be generated.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public abstract class AbstractSyncedEntity implements SyncedEntity {

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_synced", nullable = false)
    private boolean synced;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
