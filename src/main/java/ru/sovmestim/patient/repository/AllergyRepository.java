package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.patient.domain.Allergy;

/**
 * Repository for {@link Allergy} directory entities.
 */
public interface AllergyRepository extends JpaRepository<Allergy, UUID> {

    /**
     * Finds an allergy directory entry by its name ignoring case.
     *
     * @param name the allergy name to look up
     * @return the allergy entry if present
     */
    Optional<Allergy> findByNameIgnoreCase(String name);
}
