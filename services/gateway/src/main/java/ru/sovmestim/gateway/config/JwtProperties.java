package ru.sovmestim.gateway.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings shared with the identity service. The gateway only verifies access tokens; the
 * identity service signs them, so both processes receive the same secret from the environment.
 *
 * @param secret the HS256 signing secret
 * @param issuer the expected issuer claim
 * @param accessTtl access token time-to-live
 * @param refreshTtl refresh token time-to-live
 */
@ConfigurationProperties(prefix = "sovmestim.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {

    /**
     * Creates the properties, filling in defaults for missing values.
     */
    public JwtProperties {
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(30);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "sovmestim";
        }
    }
}
