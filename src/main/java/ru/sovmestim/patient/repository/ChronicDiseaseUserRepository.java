package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.patient.domain.ChronicDiseaseUser;

/**
 * Repository for {@link ChronicDiseaseUser} entities.
 */
public interface ChronicDiseaseUserRepository extends JpaRepository<ChronicDiseaseUser, UUID> {

    /**
     * Finds the active chronic conditions of a patient, newest first.
     *
     * @param userId the patient whose conditions are loaded
     * @return the patient's active chronic conditions
     */
    @Query("""
            select cu from ChronicDiseaseUser cu
            join fetch cu.chronicDisease d
            left join fetch d.mkb
            left join fetch cu.status
            where cu.user.id = :userId and cu.deleted = false
            order by cu.updatedAt desc
            """)
    List<ChronicDiseaseUser> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Finds the chronic conditions of a patient changed after the given instant.
     *
     * @param userId the patient whose conditions are loaded
     * @param since the instant after which changes are reported
     * @return the changed conditions in ascending update order
     */
    @Query("""
            select cu from ChronicDiseaseUser cu
            join fetch cu.chronicDisease d
            left join fetch d.mkb
            left join fetch cu.status
            where cu.user.id = :userId and cu.updatedAt > :since
            order by cu.updatedAt asc
            """)
    List<ChronicDiseaseUser> findChangedSince(@Param("userId") UUID userId, @Param("since") java.time.Instant since);

    /**
     * Finds a specific condition record of a patient.
     *
     * @param id the id of the condition record
     * @param userId the patient the record belongs to
     * @return the condition record if present
     */
    Optional<ChronicDiseaseUser> findByIdAndUserId(UUID id, UUID userId);
}
