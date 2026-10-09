package ru.sovmestim.sync.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.sync.domain.SyncRequest;

/**
 * Repository for stored sync push responses used for idempotency.
 */
public interface SyncRequestRepository extends JpaRepository<SyncRequest, UUID> {

    /**
     * Finds the stored response for a patient and idempotency key.
     *
     * @param userId the patient's user id
     * @param idempotencyKey the idempotency key of the push
     * @return the stored sync request, or empty when the key is unknown
     */
    Optional<SyncRequest> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
