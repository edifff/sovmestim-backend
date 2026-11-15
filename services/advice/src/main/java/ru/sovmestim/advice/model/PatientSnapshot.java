package ru.sovmestim.advice.model;

import java.util.List;
import java.util.UUID;

/**
 * Immutable view of the patient at check time: current medications, allergies and conditions.
 *
 * @param userId id of the patient
 * @param currentSubstances substances of the currently taken medications
 * @param allergies declared drug allergies
 * @param conditions declared medical conditions
 */
public record PatientSnapshot(
        UUID userId,
        List<SubstanceRef> currentSubstances,
        List<PatientAllergy> allergies,
        List<PatientCondition> conditions) {

    /**
     * Builds an empty snapshot for a user without any recorded data.
     *
     * @param userId id of the patient
     * @return snapshot with empty medication, allergy and condition lists
     */
    public static PatientSnapshot empty(UUID userId) {
        return new PatientSnapshot(userId, List.of(), List.of(), List.of());
    }
}
