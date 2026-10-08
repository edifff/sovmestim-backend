package ru.sovmestim.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development {@link OtpSender} that only writes a masked destination to the log. The code itself is
 * never logged (the demo {@code debug-return-code} switch is the supported way to read it).
 */
@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String destination, String code) {
        log.info("OTP requested for {}", mask(destination));
    }

    static String mask(String value) {
        if (value == null || value.length() < 3) {
            return "***";
        }
        int at = value.indexOf('@');
        String local = at > 0 ? value.substring(0, at) : value;
        String domain = at > 0 ? value.substring(at) : "";
        String head = local.substring(0, Math.min(1, local.length()));
        return head + "***" + domain;
    }
}
