package ru.sovmestim.patient.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.patient.domain.Mkb;

/**
 * Repository for {@link Mkb} ICD-10 code entities.
 */
public interface MkbRepository extends JpaRepository<Mkb, UUID> {

    /**
     * Finds an ICD-10 code by its code ignoring case.
     *
     * @param code the ICD-10 code to look up
     * @return the code entry if present
     */
    Optional<Mkb> findByCodeIgnoreCase(String code);
}
