package ru.sovmestim.identity.dto;

import java.time.Instant;

/**
 * Response returned after a one-time login code has been requested.
 *
 * @param email normalized e-mail address the code was sent to.
 * @param expiresAt moment the code stops being accepted.
 * @param devCode populated only when {@code sovmestim.otp.debug-return-code} is enabled.
 */
public record RequestCodeResponse(String email, Instant expiresAt, String devCode) { }
