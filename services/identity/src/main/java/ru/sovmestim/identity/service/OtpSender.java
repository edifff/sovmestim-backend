package ru.sovmestim.identity.service;

/**
 * Delivers a one-time code to the user. Implementations are free to use e-mail (SMTP), a Telegram
 * bot or SMS; the authentication flow does not care which one is wired in.
 */
public interface OtpSender {

    /**
     * Delivers the one-time code to the given destination.
     *
     * @param destination e-mail address or phone number the code is sent to.
     * @param code the one-time code to deliver.
     */
    void send(String destination, String code);
}
