package ru.sovmestim.sync.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ru.sovmestim.common.persistence.SyncedEntity;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.sync.dto.SyncChange;

/**
 * Applies one sync change through the shared flow: id check, ownership lookup, deletion, conflict
 * detection and field update. The type-specific parts are delegated to a {@link SyncChangeHandler}.
 */
@Component
public class SyncedChangeApplier {

    private static final String ERROR_ID_REQUIRED = "id is required";

    /**
     * Applies a single change.
     *
     * @param user the owning patient
     * @param change the incoming change
     * @param handler the type-specific handler
     * @param <C> the change type
     * @param <E> the entity type
     * @return the outcome reported to the client
     */
    public <C extends SyncChange, E extends SyncedEntity> SyncApplyOutcome apply(
            AppUser user, C change, SyncChangeHandler<C, E> handler) {
        if (change.id() == null) {
            return SyncApplyOutcome.rejected(handler.type(), null, ERROR_ID_REQUIRED);
        }
        Optional<E> existing = handler.findOwned(user.getId(), change);
        if (existing.isPresent()) {
            E entity = existing.get();
            if (change.deleted()) {
                entity.markDeletedSynced();
                handler.save(entity);
                return deletedOutcome(handler, entity.getId());
            }
            if (isServerNewer(entity.getUpdatedAt(), change.updatedAt())) {
                return SyncApplyOutcome.conflict(handler.type(), entity.getId());
            }
            handler.applyFields(entity, change);
            entity.setDeleted(false);
            entity.setSynced(true);
            handler.save(entity);
            return appliedOutcome(handler, entity.getId());
        }
        if (change.deleted()) {
            return SyncApplyOutcome.noop(handler.type(), change.id());
        }
        E created = handler.create(user, change);
        handler.save(created);
        return appliedOutcome(handler, created.getId());
    }

    private static <C extends SyncChange, E extends SyncedEntity> SyncApplyOutcome appliedOutcome(
            SyncChangeHandler<C, E> handler, UUID id) {
        return handler.profileChange()
                ? SyncApplyOutcome.profileApplied(handler.type(), id)
                : SyncApplyOutcome.medicationApplied(handler.type(), id);
    }

    private static <C extends SyncChange, E extends SyncedEntity> SyncApplyOutcome deletedOutcome(
            SyncChangeHandler<C, E> handler, UUID id) {
        return handler.profileChange()
                ? SyncApplyOutcome.profileApplied(handler.type(), id)
                : SyncApplyOutcome.noop(handler.type(), id);
    }

    private static boolean isServerNewer(Instant server, Instant client) {
        return client != null && server != null && server.isAfter(client);
    }
}
