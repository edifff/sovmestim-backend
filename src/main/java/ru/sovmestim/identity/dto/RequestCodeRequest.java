package ru.sovmestim.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestCodeRequest(@Email @NotBlank String email) {}
