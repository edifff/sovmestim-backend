package ru.sovmestim.advice.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Access to current medications needed to build the advice snapshot and to recheck courses.
 */
public interface IntakeClient {

    /**
     * Lists the active medication courses of the patient.
     *
     * @param userId the patient's user id
     * @return the active courses
     */
    List<MedicationInfo> activeMedications(UUID userId);

    /**
     * Lists the substance-level intake rules of the patient.
     *
     * @param userId the patient's user id
     * @return the active substance rules
     */
    List<TakenSubstanceRef> activeSubstances(UUID userId);

    /**
     * Lists the ids of the active courses.
     *
     * @param userId the patient's user id
     * @return the active course ids
     */
    List<UUID> activeCourseIds(UUID userId);

    /**
     * Maps course ids to catalog medicine ids.
     *
     * @param userId the patient's user id
     * @param courseIds the course ids
     * @return medicine id per course, skipping drafts
     */
    Map<UUID, UUID> medicineIdsForCourses(UUID userId, Collection<UUID> courseIds);
}
