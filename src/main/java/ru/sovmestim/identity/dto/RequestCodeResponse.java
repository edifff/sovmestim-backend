package ru.sovmestim.identity.dto;

import java.time.Instant;

/**
 * @param devCode populated only when {@code sovmestim.otp.debug-return-code} is enabled
 */
public record RequestCodeResponse(String email, Instant expiresAt, String devCode) {}
