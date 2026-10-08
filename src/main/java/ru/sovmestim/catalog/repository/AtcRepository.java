package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.catalog.domain.Atc;

public interface AtcRepository extends JpaRepository<Atc, UUID> {

    Optional<Atc> findByName(String name);
}
