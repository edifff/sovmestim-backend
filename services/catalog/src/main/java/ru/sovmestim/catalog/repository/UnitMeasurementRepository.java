package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.catalog.domain.UnitMeasurement;

/**
 * Repository for dosage unit measurement entries.
 */
public interface UnitMeasurementRepository extends JpaRepository<UnitMeasurement, UUID> {

    /**
     * Finds a unit of measurement by its exact name.
     *
     * @param name the unit name
     * @return the unit of measurement if present
     */
    Optional<UnitMeasurement> findByName(String name);
}
