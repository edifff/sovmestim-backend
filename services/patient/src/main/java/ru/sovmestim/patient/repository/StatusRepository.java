package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.patient.domain.Status;

/**
 * Repository for {@link Status} entities.
 */
public interface StatusRepository extends JpaRepository<Status, UUID> {

    /**
     * Finds a disease status by its name ignoring case.
     *
     * @param name the status name to look up
     * @return the status entry if present
     */
    Optional<Status> findByNameIgnoreCase(String name);
}
