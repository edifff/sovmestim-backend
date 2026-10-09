package ru.sovmestim.intake.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.intake.domain.TakenSubstance;

/**
 * Repository for substance-level intake rules.
 */
public interface TakenSubstanceRepository extends JpaRepository<TakenSubstance, UUID> {

    /**
     * Finds the active substance rules of a patient's non-deleted courses.
     *
     * @param userId the patient's user id
     * @return the active substance rules
     */
    @Query("""
            select ts from TakenSubstance ts
            left join fetch ts.activeSubstance s
            left join fetch s.atc
            left join fetch ts.medicine
            where ts.intake.user.id = :userId and ts.deleted = false
            """)
    List<TakenSubstance> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Finds the active substance rules of the given courses.
     *
     * @param intakeIds ids of the courses to load rules for
     * @return the active substance rules of the given courses
     */
    @Query("""
            select ts from TakenSubstance ts
            left join fetch ts.activeSubstance s
            left join fetch s.atc
            left join fetch ts.medicine
            where ts.intake.id in :intakeIds and ts.deleted = false
            """)
    List<TakenSubstance> findActiveByIntakeIds(@Param("intakeIds") Collection<UUID> intakeIds);

    /**
     * Finds an intake rule by its id when it belongs to the given patient.
     *
     * @param id intake rule id
     * @param userId the patient's user id
     * @return the intake rule, or empty when missing or owned by another patient
     */
    Optional<TakenSubstance> findByIdAndIntakeUserId(UUID id, UUID userId);
}
