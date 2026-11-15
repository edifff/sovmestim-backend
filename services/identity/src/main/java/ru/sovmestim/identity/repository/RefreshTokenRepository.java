package ru.sovmestim.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.identity.domain.RefreshToken;

/**
 * Repository for {@link RefreshToken} entities.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds the refresh token stored under the given hash.
     *
     * @param tokenHash the SHA-256 hash of the raw refresh token.
     * @return the stored token if present.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
