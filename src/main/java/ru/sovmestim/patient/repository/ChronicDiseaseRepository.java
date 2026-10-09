package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.patient.domain.ChronicDisease;

/**
 * Repository for {@link ChronicDisease} directory entities.
 */
public interface ChronicDiseaseRepository extends JpaRepository<ChronicDisease, UUID> {

    /**
     * Finds a chronic disease directory entry by its name ignoring case.
     *
     * @param name the disease name to look up
     * @return the disease entry if present
     */
    Optional<ChronicDisease> findByNameIgnoreCase(String name);
}
