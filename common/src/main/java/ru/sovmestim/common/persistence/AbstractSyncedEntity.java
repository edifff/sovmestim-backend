package ru.sovmestim.common.persistence;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class for the mutable, synchronized entities. Holds the audit and sync columns that are
 * identical for every such entity; {@code updatedAt} is stamped by Spring Data JPA auditing from the
 * shared {@link java.time.Clock}.
 *
 * <p>The {@code id} is intentionally not declared here: some entities use a client-assigned UUID
 * (for sync idempotency) while others let the identifier be generated.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractSyncedEntity implements SyncedEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_synced", nullable = false)
    private boolean synced;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
