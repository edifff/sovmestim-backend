package ru.sovmestim.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record AllergyRequest(
        @NotBlank String name, String severity, String symptoms, String reason) {}
