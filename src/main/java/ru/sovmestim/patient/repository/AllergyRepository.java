package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.patient.domain.Allergy;

public interface AllergyRepository extends JpaRepository<Allergy, UUID> {

    Optional<Allergy> findByNameIgnoreCase(String name);
}
