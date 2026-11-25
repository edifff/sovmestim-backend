package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.catalog.domain.FormRelease;

/**
 * Repository for medicine release form entries.
 */
public interface FormReleaseRepository extends JpaRepository<FormRelease, UUID> {

    /**
     * Finds a release form by its exact name.
     *
     * @param name the release form name
     * @return the release form if present
     */
    Optional<FormRelease> findByName(String name);
}
