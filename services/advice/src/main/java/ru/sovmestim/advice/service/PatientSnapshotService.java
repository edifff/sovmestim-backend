package ru.sovmestim.advice.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.client.CatalogClient;
import ru.sovmestim.advice.client.IntakeClient;
import ru.sovmestim.advice.client.MedicationInfo;
import ru.sovmestim.advice.client.PatientClient;
import ru.sovmestim.advice.client.TakenSubstanceRef;
import ru.sovmestim.advice.model.PatientSnapshot;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Builds the immutable {@link PatientSnapshot} used by the advice engine by calling the patient,
 * intake and catalog services; no profile or medication table is read locally.
 */
@Service
@RequiredArgsConstructor
public class PatientSnapshotService {

    private final PatientClient patientClient;
    private final IntakeClient intakeClient;
    private final CatalogClient catalogClient;

    /**
     * Builds a snapshot for the patient including all of their current treatment courses.
     *
     * @param userId the patient to build the snapshot for
     * @return the assembled patient snapshot
     */
    public PatientSnapshot snapshot(UUID userId) {
        return snapshot(userId, null);
    }

    /**
     * Builds a snapshot for the patient while omitting a single course from the current substances.
     *
     * @param userId the patient to build the snapshot for
     * @param excludeCourseMedicineId a course to omit from the current list, typically the one being
     *                                checked, so it is not reported as a duplicate of itself
     * @return the assembled patient snapshot
     */
    public PatientSnapshot snapshot(UUID userId, UUID excludeCourseMedicineId) {
        Map<String, SubstanceRef> byName = new LinkedHashMap<>();
        List<UUID> substanceIds = new ArrayList<>();
        List<UUID> medicineIds = new ArrayList<>();
        for (TakenSubstanceRef ref : intakeClient.activeSubstances(userId)) {
            if (ref.activeSubstanceId() != null) {
                substanceIds.add(ref.activeSubstanceId());
            } else if (ref.medicineId() != null) {
                medicineIds.add(ref.medicineId());
            }
        }
        for (MedicationInfo medication : intakeClient.activeMedications(userId)) {
            boolean isCheckedCourse = medication.id().equals(excludeCourseMedicineId);
            if (medication.medicineId() != null && !isCheckedCourse) {
                medicineIds.add(medication.medicineId());
            }
        }
        catalogClient.substancesByIds(substanceIds).forEach(substance -> add(byName, substance));
        catalogClient.substancesForMedicines(medicineIds).forEach(substance -> add(byName, substance));
        return new PatientSnapshot(
                userId, List.copyOf(byName.values()), patientClient.allergies(userId), patientClient.conditions(userId));
    }

    private static void add(Map<String, SubstanceRef> byName, SubstanceRef substance) {
        byName.putIfAbsent(NameNormalizer.normalize(substance.name()), substance);
    }
}
