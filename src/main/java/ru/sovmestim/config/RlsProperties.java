package ru.sovmestim.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RLS (licensed Russian drug registry) connection settings.
 *
 * <p>{@code mode = demo} serves recorded responses with zero paid calls; {@code mode = http} calls
 * the licensed API. The rest of the application only sees the {@code InteractionSource} interface.
 */
@ConfigurationProperties(prefix = "sovmestim.rls")
public record RlsProperties(
        String mode,
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout) {

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

    public boolean httpMode() {
        return "http".equalsIgnoreCase(mode);
    }
}
