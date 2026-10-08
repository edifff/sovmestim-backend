package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.patient.domain.Mkb;

public interface MkbRepository extends JpaRepository<Mkb, UUID> {

    Optional<Mkb> findByCodeIgnoreCase(String code);
}
