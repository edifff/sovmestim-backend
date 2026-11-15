package ru.sovmestim.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating or updating a chronic condition.
 *
 * @param name disease name, required
 * @param mkbCode optional ICD-10 code
 * @param status optional disease status name
 * @param diagnosisDate optional date of diagnosis
 * @param note optional free-text note
 */
public record ConditionRequest(
        @NotBlank String name, String mkbCode, String status, LocalDate diagnosisDate, String note) {
}
