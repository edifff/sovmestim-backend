package ru.sovmestim.intake.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.intake.domain.IntakeType;

/**
 * Repository for treatment course types.
 */
public interface IntakeTypeRepository extends JpaRepository<IntakeType, UUID> {

    /**
     * Finds a course type by its name, ignoring case.
     *
     * @param name the type name
     * @return the course type, or empty when unknown
     */
    Optional<IntakeType> findByNameIgnoreCase(String name);
}
