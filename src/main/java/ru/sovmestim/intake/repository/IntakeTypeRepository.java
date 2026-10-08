package ru.sovmestim.intake.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.intake.domain.IntakeType;

public interface IntakeTypeRepository extends JpaRepository<IntakeType, UUID> {

    Optional<IntakeType> findByNameIgnoreCase(String name);
}
