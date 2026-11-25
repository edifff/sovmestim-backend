package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.patient.domain.SeverityReaction;

/**
 * Repository for {@link SeverityReaction} entities.
 */
public interface SeverityReactionRepository extends JpaRepository<SeverityReaction, UUID> {

    /**
     * Finds a reaction severity by its name ignoring case.
     *
     * @param name the severity name to look up
     * @return the severity entry if present
     */
    Optional<SeverityReaction> findByNameIgnoreCase(String name);
}
