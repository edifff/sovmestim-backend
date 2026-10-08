package ru.sovmestim.sync.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.sync.domain.SyncRequest;

public interface SyncRequestRepository extends JpaRepository<SyncRequest, UUID> {

    Optional<SyncRequest> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
