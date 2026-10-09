package ru.sovmestim.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time password (e-mail code / Telegram code) settings.
 *
 * @param ttl code time-to-live
 * @param debugReturnCode whether the generated code is returned in the response for debugging
 */
@ConfigurationProperties(prefix = "sovmestim.otp")
public record OtpProperties(Duration ttl, boolean debugReturnCode) {

    /**
     * Creates the properties, defaulting the time-to-live when it is not configured.
     */
    public OtpProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(10);
        }
    }
}
