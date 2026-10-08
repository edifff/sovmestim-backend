package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.patient.domain.Status;

public interface StatusRepository extends JpaRepository<Status, UUID> {

    Optional<Status> findByNameIgnoreCase(String name);
}
