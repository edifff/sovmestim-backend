package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Client → server batch. {@code idempotencyKey} makes retries safe: a repeated batch returns the
 * stored response instead of re-applying changes and re-running advice checks.
 *
 * @param idempotencyKey key that makes retries safe; blank keys are ignored
 * @param allergies allergy changes to apply
 * @param conditions condition changes to apply
 * @param medications medication changes to apply
 */
public record SyncPushRequest(
        String idempotencyKey,
        List<AllergyChange> allergies,
        List<ConditionChange> conditions,
        List<MedicationChange> medications) {

    /**
     * Returns the allergy changes, treating a missing list as empty.
     *
     * @return the allergy changes, or an empty list
     */
    public List<AllergyChange> allergiesOrEmpty() {
        return allergies != null ? allergies : List.of();
    }

    /**
     * Returns the condition changes, treating a missing list as empty.
     *
     * @return the condition changes, or an empty list
     */
    public List<ConditionChange> conditionsOrEmpty() {
        return conditions != null ? conditions : List.of();
    }

    /**
     * Returns the medication changes, treating a missing list as empty.
     *
     * @return the medication changes, or an empty list
     */
    public List<MedicationChange> medicationsOrEmpty() {
        return medications != null ? medications : List.of();
    }

    /**
     * A single allergy change identified by a client-generated id.
     *
     * @param id client-generated UUIDv7; required so sync is idempotent
     * @param updatedAt client modification time used for conflict detection
     * @param deleted whether the allergy is deleted on the client
     * @param name allergy name
     * @param severity reaction severity name
     * @param symptoms symptom description
     * @param reason reason for the allergy
     */
    public record AllergyChange(
            UUID id, Instant updatedAt, boolean deleted, String name, String severity, String symptoms, String reason) { }

    /**
     * A single chronic condition change identified by a client-generated id.
     *
     * @param id client-generated UUIDv7; required so sync is idempotent
     * @param updatedAt client modification time used for conflict detection
     * @param deleted whether the condition is deleted on the client
     * @param name condition name
     * @param mkbCode MKB code of the condition
     * @param status status name
     * @param diagnosisDate date of diagnosis
     * @param note free-form note
     */
    public record ConditionChange(
            UUID id,
            Instant updatedAt,
            boolean deleted,
            String name,
            String mkbCode,
            String status,
            LocalDate diagnosisDate,
            String note) { }

    /**
     * A current drug course. Either {@code medicineId} (known drug) or {@code drugName} (entered by
     * the patient, may be unresolved) is used to bind the course to a catalog medicine.
     *
     * @param id client-generated UUIDv7; required so sync is idempotent
     * @param updatedAt client modification time used for conflict detection
     * @param deleted whether the course is deleted on the client
     * @param medicineId catalog medicine id, when the drug is already known
     * @param drugName name entered by the patient, when the drug is not resolved yet
     * @param dosage dosage text
     * @param frequency frequency text
     * @param startDate course start date
     * @param status status name
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
            String status) { }
}
