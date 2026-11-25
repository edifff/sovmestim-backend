package ru.sovmestim.gateway.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model of a medication course returned to the client.
 *
 * @param id course id
 * @param medicineId catalog medicine id, or {@code null} for an unresolved draft
 * @param medicineName catalog medicine name
 * @param brand trade mark brand, or {@code null}
 * @param dosage dosage text
 * @param frequency frequency text
 * @param startDate course start date
 * @param status status name, or {@code null}
 * @param updatedAt last modification time
 */
public record CourseMedicineView(
        UUID id,
        UUID medicineId,
        String medicineName,
        String brand,
        String dosage,
        String frequency,
        LocalDate startDate,
        String status,
        Instant updatedAt) { }
