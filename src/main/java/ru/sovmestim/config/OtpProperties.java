package ru.sovmestim.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-time password (e-mail code / Telegram code) settings.
 *
 * @param channel delivery channel, {@code log} (development) or {@code smtp}
 * @param from sender address of e-mail codes, required when {@code channel = smtp}
 * @param ttl code time-to-live
 * @param debugReturnCode whether the generated code is returned in the response for debugging
 */
@ConfigurationProperties(prefix = "sovmestim.otp")
public record OtpProperties(String channel, String from, Duration ttl, boolean debugReturnCode) {

    /**
     * Creates the properties, filling in defaults for missing values.
     */
    public OtpProperties {
        if (channel == null || channel.isBlank()) {
            channel = "log";
        }
        if (ttl == null) {
            ttl = Duration.ofMinutes(10);
        }
    }
}
