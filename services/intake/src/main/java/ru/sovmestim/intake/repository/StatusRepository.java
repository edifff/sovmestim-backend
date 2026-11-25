package ru.sovmestim.intake.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.intake.domain.Status;

/**
 * Repository for intake statuses.
 */
public interface StatusRepository extends JpaRepository<Status, UUID> {

    /**
     * Finds a status by its name, ignoring case.
     *
     * @param name the status name
     * @return the status, or empty when unknown
     */
    Optional<Status> findByNameIgnoreCase(String name);
}
