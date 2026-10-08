package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.catalog.domain.UnitMeasurement;

public interface UnitMeasurementRepository extends JpaRepository<UnitMeasurement, UUID> {

    Optional<UnitMeasurement> findByName(String name);
}
