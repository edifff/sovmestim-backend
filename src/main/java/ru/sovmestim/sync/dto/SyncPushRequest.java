package ru.sovmestim.sync.dto;

import java.util.List;

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
}
