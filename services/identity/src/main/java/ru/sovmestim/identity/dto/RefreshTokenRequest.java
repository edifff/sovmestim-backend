package ru.sovmestim.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for exchanging a refresh token for a new token pair.
 *
 * @param refreshToken refresh token presented by the client.
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) { }
