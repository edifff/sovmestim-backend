package ru.sovmestim.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.identity.domain.OtpCode;

/**
 * Repository for {@link OtpCode} entities.
 */
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    /**
     * Finds the most recent unconsumed code for the e-mail address.
     *
     * @param email the e-mail address the code was issued for.
     * @return the latest active code if present.
     */
    Optional<OtpCode> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);
}
