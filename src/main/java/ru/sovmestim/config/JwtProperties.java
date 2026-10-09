package ru.sovmestim.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings. The secret is HS256 (at least 32 bytes) and comes from a server secret file in
 * production; the bundled default is for local development only.
 *
 * @param secret the HS256 signing secret
 * @param issuer the token issuer claim
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
