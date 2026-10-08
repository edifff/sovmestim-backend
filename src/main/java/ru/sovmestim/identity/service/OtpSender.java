package ru.sovmestim.identity.service;

/**
 * Delivers a one-time code to the user. Implementations are free to use e-mail (SMTP), a Telegram
 * bot or SMS; the authentication flow does not care which one is wired in.
 */
public interface OtpSender {

    void send(String destination, String code);
}
