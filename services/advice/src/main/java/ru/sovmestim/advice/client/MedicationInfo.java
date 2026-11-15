package ru.sovmestim.advice.client;

import java.util.UUID;

/**
 * One active medication course as seen by the advice service.
 *
 * @param id the course id
 * @param medicineId the catalog medicine id, or {@code null} for a local draft
 * @param name the medicine name
 */
public record MedicationInfo(UUID id, UUID medicineId, String name) { }
