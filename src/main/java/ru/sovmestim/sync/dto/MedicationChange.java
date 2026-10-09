package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A current drug course. Either {@code medicineId} (known drug) or {@code drugName} (entered by the
 * patient, may be unresolved) is used to bind the course to a catalog medicine.
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
        String status)
        implements SyncChange { }
