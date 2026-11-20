package ru.sovmestim.identity.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for the destination masking used by the development OTP sender.
 */
class LoggingOtpSenderTest {

    private static final String MASK = "***";

    @Test
    void masksLocalPartAndKeepsDomain() {
        Assertions.assertThat(LoggingOtpSender.mask("user@example.com")).isEqualTo("u***@example.com");
    }

    @Test
    void masksValueWithoutDomain() {
        Assertions.assertThat(LoggingOtpSender.mask("abcdef")).isEqualTo("a***");
    }

    @Test
    void masksTooShortAndMissingValuesCompletely() {
        Assertions.assertThat(LoggingOtpSender.mask("ab")).isEqualTo(MASK);
        Assertions.assertThat(LoggingOtpSender.mask(null)).isEqualTo(MASK);
    }
}
