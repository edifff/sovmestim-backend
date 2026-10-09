package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.patient.domain.AllergyUser;

/**
 * Repository for {@link AllergyUser} entities.
 */
public interface AllergyUserRepository extends JpaRepository<AllergyUser, UUID> {

    /**
     * Finds the active allergies of a patient, newest first.
     *
     * @param userId the patient whose allergies are loaded
     * @return the patient's active allergies
     */
    @Query("""
            select au from AllergyUser au
            join fetch au.allergy
            left join fetch au.severityReaction
            where au.user.id = :userId and au.deleted = false
            order by au.updatedAt desc
            """)
    List<AllergyUser> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Finds the allergies of a patient changed after the given instant.
     *
     * @param userId the patient whose allergies are loaded
     * @param since the instant after which changes are reported
     * @return the changed allergies in ascending update order
     */
    @Query("""
            select au from AllergyUser au
            join fetch au.allergy
            left join fetch au.severityReaction
            where au.user.id = :userId and au.updatedAt > :since
            order by au.updatedAt asc
            """)
    List<AllergyUser> findChangedSince(@Param("userId") UUID userId, @Param("since") java.time.Instant since);

    /**
     * Finds a specific allergy record of a patient.
     *
     * @param id the id of the allergy record
     * @param userId the patient the record belongs to
     * @return the allergy record if present
     */
    Optional<AllergyUser> findByIdAndUserId(UUID id, UUID userId);
}
