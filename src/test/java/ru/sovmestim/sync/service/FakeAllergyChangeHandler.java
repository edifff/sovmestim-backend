package ru.sovmestim.sync.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.patient.domain.AllergyUser;

/**
 * In-memory {@link SyncChangeHandler} that stands in for a real per-type handler in tests.
 */
final class FakeAllergyChangeHandler implements SyncChangeHandler<FakeChange, AllergyUser> {

    private final Map<UUID, AllergyUser> store = new HashMap<>();
    private final boolean profileChange;

    FakeAllergyChangeHandler(boolean profileChange) {
        this.profileChange = profileChange;
    }

    Map<UUID, AllergyUser> store() {
        return store;
    }

    @Override
    public String type() {
        return "test";
    }

    @Override
    public Optional<AllergyUser> findOwned(UUID userId, FakeChange change) {
        return Optional.ofNullable(store.get(change.id()));
    }

    @Override
    public void applyFields(AllergyUser entity, FakeChange change) {
        entity.setSymptoms("updated");
    }

    @Override
    public AllergyUser create(AppUser user, FakeChange change) {
        return AllergyUser.builder().id(change.id()).user(user).build();
    }

    @Override
    public void save(AllergyUser entity) {
        store.put(entity.getId(), entity);
    }

    @Override
    public boolean profileChange() {
        return profileChange;
    }
}
