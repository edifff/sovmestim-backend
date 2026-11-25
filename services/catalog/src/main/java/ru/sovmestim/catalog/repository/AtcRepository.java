package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.catalog.domain.Atc;

/**
 * Repository for ATC classification entries.
 */
public interface AtcRepository extends JpaRepository<Atc, UUID> {

    /**
     * Finds an ATC entry by its exact name.
     *
     * @param name the ATC name
     * @return the ATC entry if present
     */
    Optional<Atc> findByName(String name);
}
