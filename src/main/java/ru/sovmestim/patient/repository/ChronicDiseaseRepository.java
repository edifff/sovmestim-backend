package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.patient.domain.ChronicDisease;

public interface ChronicDiseaseRepository extends JpaRepository<ChronicDisease, UUID> {

    Optional<ChronicDisease> findByNameIgnoreCase(String name);
}
