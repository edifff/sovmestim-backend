package ru.sovmestim.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.identity.domain.Gender;

/**
 * Repository for {@link Gender} entities.
 */
public interface GenderRepository extends JpaRepository<Gender, UUID> {

    /**
     * Finds the gender entry with the given name.
     *
     * @param name the gender name to look up.
     * @return the gender entry if present.
     */
    Optional<Gender> findByName(String name);
}
