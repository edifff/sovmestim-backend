package ru.sovmestim.patient.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating or updating an allergy.
 *
 * @param name allergy name, required
 * @param severity optional reaction severity name
 * @param symptoms optional reported symptoms
 * @param reason optional allergy cause
 */
public record AllergyRequest(
        @NotBlank String name, String severity, String symptoms, String reason) {
}
