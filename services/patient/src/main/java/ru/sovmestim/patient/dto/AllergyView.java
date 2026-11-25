package ru.sovmestim.patient.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * View of an allergy recorded for a patient.
 *
 * @param id the allergy record id
 * @param name allergy name
 * @param code allergy code, may be null
 * @param severity reaction severity name, may be null
 * @param symptoms reported symptoms, may be null
 * @param reason allergy cause, may be null
 * @param updatedAt last update timestamp
 */
public record AllergyView(
        UUID id, String name, String code, String severity, String symptoms, String reason, Instant updatedAt) {
}
