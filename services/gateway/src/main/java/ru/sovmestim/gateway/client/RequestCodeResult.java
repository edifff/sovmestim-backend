package ru.sovmestim.gateway.client;

import java.time.Instant;

/**
 * Result of requesting a one-time login code.
 *
 * @param email the normalized e-mail address
 * @param expiresAt the moment the code stops being accepted
 * @param devCode the code itself in dev mode, or {@code null}
 */
public record RequestCodeResult(String email, Instant expiresAt, String devCode) { }
