package ru.sovmestim.intake.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Either {@code medicineId} (known drug) or {@code drugName} (entered by the patient, may be
 * unresolved and stays {@code DRAFT_UNVERIFIED} until the catalog knows it).
 *
 * @param medicineId catalog medicine id, when the drug is already known
 * @param drugName name entered by the patient, when the drug is not resolved yet
 * @param dosage dosage text
 * @param frequency frequency text
 * @param startDate course start date
 */
public record CourseMedicineRequest(
        UUID medicineId, String drugName, String dosage, String frequency, LocalDate startDate) { }
