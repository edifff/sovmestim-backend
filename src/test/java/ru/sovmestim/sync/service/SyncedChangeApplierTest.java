package ru.sovmestim.sync.service;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.patient.domain.AllergyUser;

/**
 * Tests for the shared sync apply flow: id check, conflict detection, create, update and delete.
 */
class SyncedChangeApplierTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant OLDER = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant NEWER = Instant.parse("2027-01-01T00:00:00Z");
    private static final String UPDATED_SYMPTOMS = "updated";

    private final SyncedChangeApplier applier = new SyncedChangeApplier();
    private final AppUser user = AppUser.builder().id(UUID.randomUUID()).build();

    @Test
    void rejectsChangeWithoutId() {
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(true);

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(null, NOW, false), handler);

        Assertions.assertThat(outcome.result().status()).isEqualTo(SyncApplyOutcome.REJECTED);
        Assertions.assertThat(outcome.result().message()).isEqualTo("id is required");
    }

    @Test
    void conflictsWhenServerRowIsNewer() {
        UUID id = UUID.randomUUID();
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(true);
        handler.store().put(id, entity(id, NEWER));

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(id, NOW, false), handler);

        Assertions.assertThat(outcome.result().status()).isEqualTo(SyncApplyOutcome.CONFLICT);
        Assertions.assertThat(outcome.profileChanged()).isFalse();
    }

    @Test
    void createsMissingProfileEntity() {
        UUID id = UUID.randomUUID();
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(true);

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(id, NOW, false), handler);

        Assertions.assertThat(outcome.result().status()).isEqualTo(SyncApplyOutcome.APPLIED);
        Assertions.assertThat(outcome.profileChanged()).isTrue();
        Assertions.assertThat(handler.store()).containsKey(id);
    }

    @Test
    void appliesUpdateAndMarksSynced() {
        UUID id = UUID.randomUUID();
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(true);
        AllergyUser stored = entity(id, OLDER);
        handler.store().put(id, stored);

        applier.apply(user, new FakeChange(id, NOW, false), handler);

        Assertions.assertThat(stored.getSymptoms()).isEqualTo(UPDATED_SYMPTOMS);
        Assertions.assertThat(stored.isSynced()).isTrue();
        Assertions.assertThat(stored.isDeleted()).isFalse();
    }

    @Test
    void deletesExistingProfileEntityAndFlagsProfileChange() {
        UUID id = UUID.randomUUID();
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(true);
        AllergyUser stored = entity(id, OLDER);
        handler.store().put(id, stored);

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(id, NOW, true), handler);

        Assertions.assertThat(stored.isDeleted()).isTrue();
        Assertions.assertThat(stored.isSynced()).isTrue();
        Assertions.assertThat(outcome.profileChanged()).isTrue();
    }

    @Test
    void deletingMissingEntityIsNoop() {
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(true);

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(UUID.randomUUID(), NOW, true), handler);

        Assertions.assertThat(outcome.result().status()).isEqualTo(SyncApplyOutcome.APPLIED);
        Assertions.assertThat(outcome.result().message()).isEqualTo("no-op");
        Assertions.assertThat(outcome.courseIdForCheck()).isNull();
    }

    @Test
    void medicationApplyReportsCourseForCheck() {
        UUID id = UUID.randomUUID();
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(false);

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(id, NOW, false), handler);

        Assertions.assertThat(outcome.courseIdForCheck()).isEqualTo(id);
    }

    @Test
    void medicationDeleteIsNoop() {
        UUID id = UUID.randomUUID();
        FakeAllergyChangeHandler handler = new FakeAllergyChangeHandler(false);
        handler.store().put(id, entity(id, OLDER));

        SyncApplyOutcome outcome = applier.apply(user, new FakeChange(id, NOW, true), handler);

        Assertions.assertThat(outcome.courseIdForCheck()).isNull();
        Assertions.assertThat(outcome.profileChanged()).isFalse();
    }

    private static AllergyUser entity(UUID id, Instant updatedAt) {
        return AllergyUser.builder().id(id).updatedAt(updatedAt).build();
    }
}
