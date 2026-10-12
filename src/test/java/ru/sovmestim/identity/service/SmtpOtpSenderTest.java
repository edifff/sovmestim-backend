package ru.sovmestim.identity.service;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import ru.sovmestim.config.OtpProperties;

/**
 * Tests for the SMTP one-time-code sender.
 */
class SmtpOtpSenderTest {

    private static final String FROM = "no-reply@sovmestim.test";
    private static final String DESTINATION = "patient@example.com";
    private static final String CODE = "123456";
    private static final String CHANNEL = "smtp";

    @Test
    void sendsCodeToTheDestination() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        SmtpOtpSender sender =
                new SmtpOtpSender(mailSender, new OtpProperties(CHANNEL, FROM, Duration.ofMinutes(10), false));

        sender.send(DESTINATION, CODE);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        Assertions.assertThat(message.getFrom()).isEqualTo(FROM);
        Assertions.assertThat(message.getTo()).containsExactly(DESTINATION);
        Assertions.assertThat(message.getText()).contains(CODE);
    }

    @Test
    void rejectsMissingSenderAddress() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        SmtpOtpSender sender =
                new SmtpOtpSender(mailSender, new OtpProperties(CHANNEL, null, Duration.ofMinutes(10), false));

        Assertions.assertThatThrownBy(() -> sender.send(DESTINATION, CODE))
                .isInstanceOf(IllegalStateException.class);
    }
}
