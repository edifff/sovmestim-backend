package ru.sovmestim.patient.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.PatientCondition;
import ru.sovmestim.advice.model.PatientSnapshot;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.catalog.domain.ActiveSubstance;
import ru.sovmestim.catalog.repository.SubstanceInMedicineRepository;
import ru.sovmestim.common.util.NameNormalizer;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.domain.TakenSubstance;
import ru.sovmestim.intake.repository.CourseMedicineRepository;
import ru.sovmestim.intake.repository.TakenSubstanceRepository;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.repository.AllergyUserRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseUserRepository;
import ru.sovmestim.patient.repository.UserDiseaseRepository;

/**
 * Builds the immutable {@link PatientSnapshot} used by the advice engine from the patient's stored
 * profile and current medications.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PatientSnapshotService {

    private final AllergyUserRepository allergyUserRepository;
    private final ChronicDiseaseUserRepository chronicDiseaseUserRepository;
    private final UserDiseaseRepository userDiseaseRepository;
    private final TakenSubstanceRepository takenSubstanceRepository;
    private final CourseMedicineRepository courseMedicineRepository;
    private final SubstanceInMedicineRepository substanceInMedicineRepository;

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
        return new PatientSnapshot(
                userId, currentSubstances(userId, excludeCourseMedicineId), allergies(userId), conditions(userId));
    }

    private List<SubstanceRef> currentSubstances(UUID userId, UUID excludeCourseMedicineId) {
        Map<String, SubstanceRef> byName = new LinkedHashMap<>();
        List<UUID> medicineIdsFromTaken = new java.util.ArrayList<>();
        for (TakenSubstance taken : takenSubstanceRepository.findActiveByUserId(userId)) {
            if (taken.getActiveSubstance() != null) {
                add(byName, toRef(taken.getActiveSubstance()));
            } else if (taken.getMedicine() != null) {
                medicineIdsFromTaken.add(taken.getMedicine().getId());
            }
        }
        if (!medicineIdsFromTaken.isEmpty()) {
            addMedicine(byName, medicineIdsFromTaken);
        }
        List<CourseMedicine> courses = courseMedicineRepository.findActiveByUserId(userId).stream()
                .filter(course -> excludeCourseMedicineId == null || !excludeCourseMedicineId.equals(course.getId()))
                .toList();
        if (!courses.isEmpty()) {
            List<UUID> medicineIds = courses.stream().map(course -> course.getMedicine().getId()).toList();
            addMedicine(byName, medicineIds);
        }
        return List.copyOf(byName.values());
    }

    private void addMedicine(Map<String, SubstanceRef> byName, List<UUID> medicineIds) {
        substanceInMedicineRepository.findByMedicineIdIn(medicineIds)
                .forEach(link -> add(byName, toRef(link.getActiveSubstance())));
    }

    private List<PatientAllergy> allergies(UUID userId) {
        return allergyUserRepository.findActiveByUserId(userId).stream()
                .map(this::toAllergy)
                .toList();
    }

    private PatientAllergy toAllergy(AllergyUser allergyUser) {
        String severity =
                allergyUser.getSeverityReaction() != null ? allergyUser.getSeverityReaction().getName() : null;
        return new PatientAllergy(
                allergyUser.getAllergy().getName(), allergyUser.getAllergy().getCode(), severity);
    }

    private List<PatientCondition> conditions(UUID userId) {
        List<PatientCondition> conditions = new java.util.ArrayList<>();
        for (ChronicDiseaseUser disease : chronicDiseaseUserRepository.findActiveByUserId(userId)) {
            String code = disease.getChronicDisease().getMkb() != null
                    ? disease.getChronicDisease().getMkb().getCode()
                    : null;
            conditions.add(new PatientCondition(disease.getChronicDisease().getName(), code));
        }
        userDiseaseRepository.findByUserIdAndDeletedFalse(userId).stream()
                .filter(disease -> disease.getDiseaseName() != null)
                .map(disease -> new PatientCondition(
                        disease.getDiseaseName(), disease.getMkb() != null ? disease.getMkb().getCode() : null))
                .forEach(conditions::add);
        return conditions;
    }

    private static void add(Map<String, SubstanceRef> byName, SubstanceRef substance) {
        byName.putIfAbsent(NameNormalizer.normalize(substance.name()), substance);
    }

    private static SubstanceRef toRef(ActiveSubstance substance) {
        String atcCode = substance.getAtc() != null ? substance.getAtc().getName() : null;
        String atcName = substance.getAtc() != null ? substance.getAtc().getDescription() : null;
        return new SubstanceRef(substance.getId(), substance.getName(), atcCode, atcName, null);
    }
}
