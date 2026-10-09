package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.sovmestim.patient.domain.UserDisease;

/**
 * Repository for {@link UserDisease} entities.
 */
public interface UserDiseaseRepository extends JpaRepository<UserDisease, UUID> {

    /**
     * Finds the active diseases entered by the patient.
     *
     * @param userId the patient whose diseases are loaded
     * @return the patient's active self-entered diseases
     */
    @Query("""
            select d from UserDisease d
            left join fetch d.mkb
            where d.user.id = :userId and d.deleted = false
            """)
    List<UserDisease> findByUserIdAndDeletedFalse(@Param("userId") UUID userId);
}
