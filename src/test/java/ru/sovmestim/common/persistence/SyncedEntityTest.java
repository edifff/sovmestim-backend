package ru.sovmestim.common.persistence;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.sovmestim.patient.domain.AllergyUser;

/**
 * Tests for the soft-delete defaults of {@link SyncedEntity}.
 */
class SyncedEntityTest {

    @Test
    void markDeletedFlagsLocalChangeAsPendingSync() {
        AllergyUser entity = AllergyUser.builder().id(UUID.randomUUID()).build();

        entity.markDeleted();

        Assertions.assertThat(entity.isDeleted()).isTrue();
        Assertions.assertThat(entity.isSynced()).isFalse();
    }

    @Test
    void markDeletedSyncedFlagsRemoteDeletionAsSynced() {
        AllergyUser entity = AllergyUser.builder().id(UUID.randomUUID()).build();

        entity.markDeletedSynced();

        Assertions.assertThat(entity.isDeleted()).isTrue();
        Assertions.assertThat(entity.isSynced()).isTrue();
    }
}
