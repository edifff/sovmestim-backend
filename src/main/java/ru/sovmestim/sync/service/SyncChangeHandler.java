package ru.sovmestim.sync.service;

import java.util.Optional;
import java.util.UUID;

import ru.sovmestim.common.persistence.SyncedEntity;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.sync.dto.SyncChange;

/**
 * Type-specific part of applying a sync change: lookup, field mapping and persistence. The shared
 * id/conflict/deletion flow lives in {@link SyncedChangeApplier}.
 *
 * @param <C> the change type
 * @param <E> the entity type
 */
public interface SyncChangeHandler<C extends SyncChange, E extends SyncedEntity> {

    /**
     * Returns the entity type reported to the client.
     *
     * @return the type name, such as {@code allergy}
     */
    String type();

    /**
     * Loads the stored entity owned by the patient, if any.
     *
     * @param userId the patient's user id
     * @param change the incoming change
     * @return the stored entity, or empty when none exists
     */
    Optional<E> findOwned(UUID userId, C change);

    /**
     * Copies the change fields onto the stored entity.
     *
     * @param entity the stored entity to update
     * @param change the incoming change
     */
    void applyFields(E entity, C change);

    /**
     * Creates a new entity from the change.
     *
     * @param user the owning patient
     * @param change the incoming change
     * @return the new, unsaved entity
     */
    E create(AppUser user, C change);

    /**
     * Persists the entity.
     *
     * @param entity the entity to save
     */
    void save(E entity);

    /**
     * Tells whether changes of this type affect the patient profile (and so trigger a full advice
     * recheck) rather than a single medication course.
     *
     * @return {@code true} for profile entities
     */
    boolean profileChange();
}
