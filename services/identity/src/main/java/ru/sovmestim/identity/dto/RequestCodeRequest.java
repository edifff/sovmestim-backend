package ru.sovmestim.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for asking for a one-time login code.
 *
 * @param email e-mail address the code should be sent to.
 */
public record RequestCodeRequest(@Email @NotBlank String email) { }
