package ru.sovmestim.advice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.advice.domain.AdviceRecord;

public interface AdviceRecordRepository extends JpaRepository<AdviceRecord, UUID> {

    List<AdviceRecord> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AdviceRecord> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID userId, Instant since);
}
