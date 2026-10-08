package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.patient.domain.SeverityReaction;

public interface SeverityReactionRepository extends JpaRepository<SeverityReaction, UUID> {

    Optional<SeverityReaction> findByNameIgnoreCase(String name);
}
