package ru.sovmestim.common.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Helpers for reading the authenticated principal from a validated access token.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * Reads the user identifier from the token subject.
     *
     * @param jwt the validated access token
     * @return the authenticated user's identifier
     */
    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
