package ru.sovmestim.intake.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.intake.domain.Intake;

public interface IntakeRepository extends JpaRepository<Intake, UUID> {

    List<Intake> findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(UUID userId);
}
