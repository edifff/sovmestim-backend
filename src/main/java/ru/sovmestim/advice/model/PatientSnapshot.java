package ru.sovmestim.advice.model;

import java.util.List;
import java.util.UUID;

/**
 * Immutable view of the patient at check time: current medications, allergies and conditions.
 */
public record PatientSnapshot(
        UUID userId,
        List<SubstanceRef> currentSubstances,
        List<PatientAllergy> allergies,
        List<PatientCondition> conditions) {

    public static PatientSnapshot empty(UUID userId) {
        return new PatientSnapshot(userId, List.of(), List.of(), List.of());
    }
}
