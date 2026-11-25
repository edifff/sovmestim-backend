package ru.sovmestim.advice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.advice.domain.AdviceRecord;

/**
 * Repository for advice audit records.
 */
public interface AdviceRecordRepository extends JpaRepository<AdviceRecord, UUID> {

    /**
     * Loads the 50 most recent records of a user, newest first.
     *
     * @param userId id of the user
     * @return recent advice records
     */
    List<AdviceRecord> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Loads the user's records created after the given instant, oldest first.
     *
     * @param userId id of the user
     * @param since exclusive lower bound of the creation time
     * @return advice records in the requested window
     */
    List<AdviceRecord> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID userId, Instant since);
}
