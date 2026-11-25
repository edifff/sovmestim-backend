package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.gateway.dto.AllergyRequest;
import ru.sovmestim.gateway.dto.AllergyView;
import ru.sovmestim.gateway.dto.ConditionRequest;
import ru.sovmestim.gateway.dto.ConditionView;

/**
 * Access to the patient service.
 */
public interface PatientClient {

    /**
     * Lists the active allergies of the patient.
     *
     * @param userId the patient's user id
     * @return the active allergies
     */
    List<AllergyView> allergies(UUID userId);

    /**
     * Lists the active conditions of the patient.
     *
     * @param userId the patient's user id
     * @return the active conditions
     */
    List<ConditionView> conditions(UUID userId);

    /**
     * Creates or updates an allergy.
     *
     * @param userId the patient's user id
     * @param id the record id, or {@code null} to create
     * @param clientUpdatedAt the client modification time, or {@code null} for the interactive path
     * @param request the allergy fields
     * @return the write outcome
     */
    ChangeResult upsertAllergy(UUID userId, UUID id, Instant clientUpdatedAt, AllergyRequest request);

    /**
     * Deletes an allergy.
     *
     * @param userId the patient's user id
     * @param id the record id
     * @param clientUpdatedAt the client modification time, or {@code null} for the interactive path
     * @return the write outcome
     */
    ChangeResult deleteAllergy(UUID userId, UUID id, Instant clientUpdatedAt);

    /**
     * Creates or updates a condition.
     *
     * @param userId the patient's user id
     * @param id the record id, or {@code null} to create
     * @param clientUpdatedAt the client modification time, or {@code null} for the interactive path
     * @param request the condition fields
     * @return the write outcome
     */
    ChangeResult upsertCondition(UUID userId, UUID id, Instant clientUpdatedAt, ConditionRequest request);

    /**
     * Deletes a condition.
     *
     * @param userId the patient's user id
     * @param id the record id
     * @param clientUpdatedAt the client modification time, or {@code null} for the interactive path
     * @return the write outcome
     */
    ChangeResult deleteCondition(UUID userId, UUID id, Instant clientUpdatedAt);

    /**
     * Returns profile changes after the cursor.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor
     * @return the profile delta
     */
    PatientDelta changesSince(UUID userId, Instant cursor);
}
