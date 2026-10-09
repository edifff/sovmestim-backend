package ru.sovmestim.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.sovmestim.identity.domain.AppUser;

/**
 * Repository for {@link AppUser} entities.
 */
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    /**
     * Finds the user with the given e-mail address.
     *
     * @param email the e-mail address to look up.
     * @return the user if present.
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Checks whether a user with the given e-mail address exists.
     *
     * @param email the e-mail address to check.
     * @return {@code true} if such a user exists.
     */
    boolean existsByEmail(String email);
}
