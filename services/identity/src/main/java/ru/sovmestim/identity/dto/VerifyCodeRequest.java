package ru.sovmestim.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for verifying a one-time login code.
 *
 * @param email e-mail address the code was requested for.
 * @param code one-time code submitted by the user.
 */
public record VerifyCodeRequest(@Email @NotBlank String email, @NotBlank String code) { }
