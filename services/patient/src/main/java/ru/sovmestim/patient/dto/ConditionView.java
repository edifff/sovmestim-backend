package ru.sovmestim.patient.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * View of a chronic condition with its diagnosis details.
 *
 * @param id the condition record id
 * @param name disease name
 * @param mkbCode ICD-10 code, may be null
 * @param status disease status name, may be null
 * @param diagnosisDate date of diagnosis, may be null
 * @param note free-text note, may be null
 * @param updatedAt last update timestamp
 */
public record ConditionView(
        UUID id,
        String name,
        String mkbCode,
        String status,
        LocalDate diagnosisDate,
        String note,
        Instant updatedAt) {
}
