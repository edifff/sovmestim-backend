package ru.sovmestim.identity.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sovmestim.identity.domain.Gender;

public interface GenderRepository extends JpaRepository<Gender, UUID> {

    Optional<Gender> findByName(String name);
}
