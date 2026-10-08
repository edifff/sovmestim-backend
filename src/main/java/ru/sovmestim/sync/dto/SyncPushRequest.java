package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Client → server batch. {@code idempotencyKey} makes retries safe: a repeated batch returns the
 * stored response instead of re-applying changes and re-running advice checks.
 */
public record SyncPushRequest(
        String idempotencyKey,
        List<AllergyChange> allergies,
        List<ConditionChange> conditions,
        List<MedicationChange> medications) {

    public List<AllergyChange> allergiesOrEmpty() {
        return allergies != null ? allergies : List.of();
    }

    public List<ConditionChange> conditionsOrEmpty() {
        return conditions != null ? conditions : List.of();
    }

    public List<MedicationChange> medicationsOrEmpty() {
        return medications != null ? medications : List.of();
    }

    /**
     * @param id client-generated UUIDv7; required so sync is idempotent
     */
    public record AllergyChange(
            UUID id, Instant updatedAt, boolean deleted, String name, String severity, String symptoms, String reason) {}

    public record ConditionChange(
            UUID id,
            Instant updatedAt,
            boolean deleted,
            String name,
            String mkbCode,
            String status,
            LocalDate diagnosisDate,
            String note) {}

    /**
     * A current drug course. Either {@code medicineId} (known drug) or {@code drugName} (entered by
     * the patient, may be unresolved) is used to bind the course to a catalog medicine.
     */
    public record MedicationChange(
            UUID id,
            Instant updatedAt,
            boolean deleted,
            UUID medicineId,
            String drugName,
            String dosage,
            String frequency,
            LocalDate startDate,
            String status) {}
}
