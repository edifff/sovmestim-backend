package ru.sovmestim.identity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.config.OtpProperties;

/**
 * {@link OtpSender} that delivers the code as an e-mail over SMTP. Active only when
 * {@code sovmestim.otp.channel = smtp}; connection details come from the standard
 * {@code spring.mail.*} properties (host, port, username, password).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sovmestim.otp", name = "channel", havingValue = "smtp")
public class SmtpOtpSender implements OtpSender {

    private static final String SUBJECT = "Код входа в Совместим";

    private final JavaMailSender mailSender;
    private final OtpProperties properties;

    @Override
    public void send(String destination, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(requireFrom());
        message.setTo(destination);
        message.setSubject(SUBJECT);
        message.setText(body(code));
        mailSender.send(message);
    }

    private String requireFrom() {
        String from = properties.from();
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("sovmestim.otp.from must be set when sovmestim.otp.channel=smtp");
        }
        return from;
    }

    private String body(String code) {
        return "Ваш код для входа: " + code + "\n"
                + "Код действует " + properties.ttl().toMinutes() + " минут.\n"
                + "Если это были не вы, проигнорируйте это письмо.";
    }
}
