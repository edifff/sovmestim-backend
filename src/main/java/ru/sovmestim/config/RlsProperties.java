package ru.sovmestim.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RLS (licensed Russian drug registry) connection settings.
 *
 * <p>{@code mode = demo} serves recorded responses with zero paid calls; {@code mode = http} calls
 * the licensed API. The rest of the application only sees the {@code InteractionSource} interface.
 *
 * @param mode connection mode, {@code demo} or {@code http}
 * @param baseUrl base URL of the licensed API
 * @param apiKey API key for the licensed API
 * @param connectTimeout TCP connect timeout
 * @param readTimeout response read timeout
 */
@ConfigurationProperties(prefix = "sovmestim.rls")
public record RlsProperties(
        String mode,
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout) {

    /**
     * Creates the properties, filling in defaults for missing values.
     */
    public RlsProperties {
        if (mode == null || mode.isBlank()) {
            mode = "demo";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(2);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(5);
        }
    }

    /**
     * Tells whether the licensed HTTP API mode is selected.
     *
     * @return {@code true} when {@code mode} is {@code http}
     */
    public boolean httpMode() {
        return "http".equalsIgnoreCase(mode);
    }
}
