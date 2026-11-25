package ru.sovmestim.advice.client;

import java.util.List;
import java.util.UUID;

import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.PatientCondition;

/**
 * Access to the patient profile needed to build the advice snapshot.
 */
public interface PatientClient {

    /**
     * Lists the active allergies of the patient.
     *
     * @param userId the patient's user id
     * @return the active allergies
     */
    List<PatientAllergy> allergies(UUID userId);

    /**
     * Lists the active chronic conditions of the patient.
     *
     * @param userId the patient's user id
     * @return the active conditions
     */
    List<PatientCondition> conditions(UUID userId);
}
