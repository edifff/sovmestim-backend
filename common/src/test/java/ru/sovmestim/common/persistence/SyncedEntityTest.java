package ru.sovmestim.common.persistence;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the soft-delete defaults of {@link SyncedEntity}.
 */
class SyncedEntityTest {

    @Test
    void markDeletedFlagsLocalChangeAsPendingSync() {
        TestEntity entity = new TestEntity();

        entity.markDeleted();

        Assertions.assertThat(entity.isDeleted()).isTrue();
        Assertions.assertThat(entity.isSynced()).isFalse();
    }

    @Test
    void markDeletedSyncedFlagsRemoteDeletionAsSynced() {
        TestEntity entity = new TestEntity();

        entity.markDeletedSynced();

        Assertions.assertThat(entity.isDeleted()).isTrue();
        Assertions.assertThat(entity.isSynced()).isTrue();
    }

    /**
     * Minimal synced entity used to exercise the interface defaults without a database.
     */
    private static final class TestEntity implements SyncedEntity {

        private final UUID id = UUID.randomUUID();
        private final Instant updatedAt = Instant.EPOCH;
        private boolean deleted;
        private boolean synced;

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public Instant getUpdatedAt() {
            return updatedAt;
        }

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public boolean isSynced() {
            return synced;
        }

        @Override
        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        @Override
        public void setSynced(boolean synced) {
            this.synced = synced;
        }
    }
}
