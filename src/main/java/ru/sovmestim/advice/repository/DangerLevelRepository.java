package ru.sovmestim.advice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.advice.domain.DangerLevel;

/**
 * Repository for the danger level dictionary.
 */
public interface DangerLevelRepository extends JpaRepository<DangerLevel, UUID> {

    /**
     * Finds a danger level by its name, ignoring case.
     *
     * @param name danger level name
     * @return the danger level when present
     */
    Optional<DangerLevel> findByNameIgnoreCase(String name);
}
