package ru.sovmestim.intake.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.intake.domain.Intake;

/**
 * Repository for courses of treatment.
 */
public interface IntakeRepository extends JpaRepository<Intake, UUID> {

    /**
     * Finds the patient's non-deleted courses, newest first.
     *
     * @param userId the patient's user id
     * @return the patient's courses
     */
    List<Intake> findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(UUID userId);
}
