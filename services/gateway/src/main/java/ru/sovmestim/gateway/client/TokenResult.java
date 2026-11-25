package ru.sovmestim.gateway.client;

/**
 * Issued access and refresh tokens.
 *
 * @param accessToken short-lived JWT
 * @param refreshToken opaque rotating token
 * @param tokenType authorization scheme
 * @param expiresIn access token lifetime in seconds
 */
public record TokenResult(String accessToken, String refreshToken, String tokenType, long expiresIn) { }
