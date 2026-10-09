package ru.sovmestim.patient.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
    List<UserDisease> findByUserIdAndDeletedFalse(UUID userId);
}
