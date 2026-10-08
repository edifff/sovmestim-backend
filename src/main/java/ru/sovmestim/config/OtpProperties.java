package ru.sovmestim.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time password (e-mail code / Telegram code) settings.
 */
@ConfigurationProperties(prefix = "sovmestim.otp")
public record OtpProperties(Duration ttl, boolean debugReturnCode) {

    public OtpProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(10);
        }
    }
}
