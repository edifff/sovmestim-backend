package ru.sovmestim.advice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.advice.domain.DangerLevel;

public interface DangerLevelRepository extends JpaRepository<DangerLevel, UUID> {

    Optional<DangerLevel> findByNameIgnoreCase(String name);
}
