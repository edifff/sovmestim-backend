package ru.sovmestim.intake.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Either {@code medicineId} (known drug) or {@code drugName} (entered by the patient, may be
 * unresolved and stays {@code DRAFT_UNVERIFIED} until the catalog knows it).
 */
public record CourseMedicineRequest(
        UUID medicineId, String drugName, String dosage, String frequency, LocalDate startDate) {}
