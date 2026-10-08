package ru.sovmestim.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.catalog.domain.FormRelease;

public interface FormReleaseRepository extends JpaRepository<FormRelease, UUID> {

    Optional<FormRelease> findByName(String name);
}
