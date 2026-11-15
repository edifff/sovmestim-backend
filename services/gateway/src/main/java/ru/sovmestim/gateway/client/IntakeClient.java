package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.gateway.dto.CourseMedicineRequest;
import ru.sovmestim.gateway.dto.CourseMedicineView;

/**
 * Access to the intake service.
 */
public interface IntakeClient {

    /**
     * Lists the active medication courses of the patient.
     *
     * @param userId the patient's user id
     * @return the active courses
     */
    List<CourseMedicineView> activeMedications(UUID userId);

    /**
     * Adds a medication course.
     *
     * @param userId the patient's user id
     * @param request the course to add
     * @return the stored course
     */
    CourseMedicineView addMedication(UUID userId, CourseMedicineRequest request);

    /**
     * Deletes a medication course.
     *
     * @param userId the patient's user id
     * @param id the course id
     * @param clientUpdatedAt the client modification time, or {@code null} for the interactive path
     * @return the write outcome
     */
    ChangeResult deleteMedication(UUID userId, UUID id, Instant clientUpdatedAt);

    /**
     * Creates or updates a medication course from a sync change.
     *
     * @param userId the patient's user id
     * @param id the course id
     * @param clientUpdatedAt the client modification time
     * @param medicineId catalog medicine id, or {@code null}
     * @param drugName patient-entered name, or {@code null}
     * @param dosage dosage text
     * @param frequency frequency text
     * @param startDate start date
     * @param status status name
     * @return the write outcome
     */
    UpsertResult upsertMedication(
            UUID userId,
            UUID id,
            Instant clientUpdatedAt,
            UUID medicineId,
            String drugName,
            String dosage,
            String frequency,
            java.time.LocalDate startDate,
            String status);

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
     * @return medicine id per course
     */
    Map<UUID, UUID> medicineIdsForCourses(UUID userId, Collection<UUID> courseIds);

    /**
     * Returns medication changes after the cursor.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor
     * @return the medication delta
     */
    MedicationDelta changesSince(UUID userId, Instant cursor);
}
