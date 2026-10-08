package ru.sovmestim.patient.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class PatientSnapshotService {

    private final AllergyUserRepository allergyUserRepository;
    private final ChronicDiseaseUserRepository chronicDiseaseUserRepository;
    private final UserDiseaseRepository userDiseaseRepository;
    private final TakenSubstanceRepository takenSubstanceRepository;
    private final CourseMedicineRepository courseMedicineRepository;
    private final SubstanceInMedicineRepository substanceInMedicineRepository;

    public PatientSnapshotService(
            AllergyUserRepository allergyUserRepository,
            ChronicDiseaseUserRepository chronicDiseaseUserRepository,
            UserDiseaseRepository userDiseaseRepository,
            TakenSubstanceRepository takenSubstanceRepository,
            CourseMedicineRepository courseMedicineRepository,
            SubstanceInMedicineRepository substanceInMedicineRepository) {
        this.allergyUserRepository = allergyUserRepository;
        this.chronicDiseaseUserRepository = chronicDiseaseUserRepository;
        this.userDiseaseRepository = userDiseaseRepository;
        this.takenSubstanceRepository = takenSubstanceRepository;
        this.courseMedicineRepository = courseMedicineRepository;
        this.substanceInMedicineRepository = substanceInMedicineRepository;
    }

    public PatientSnapshot snapshot(UUID userId) {
        return snapshot(userId, null);
    }

    /**
     * @param excludeCourseMedicineId a course to omit from the current list, typically the one being
     *                                checked, so it is not reported as a duplicate of itself
     */
    public PatientSnapshot snapshot(UUID userId, UUID excludeCourseMedicineId) {
        return new PatientSnapshot(
                userId, currentSubstances(userId, excludeCourseMedicineId), allergies(userId), conditions(userId));
    }

    private List<SubstanceRef> currentSubstances(UUID userId, UUID excludeCourseMedicineId) {
        Map<String, SubstanceRef> byName = new LinkedHashMap<>();
        for (TakenSubstance taken : takenSubstanceRepository.findActiveByUserId(userId)) {
            if (taken.getActiveSubstance() != null) {
                add(byName, toRef(taken.getActiveSubstance()));
            } else if (taken.getMedicine() != null) {
                addMedicine(byName, taken.getMedicine().getId());
            }
        }
        for (CourseMedicine course : courseMedicineRepository.findActiveByUserId(userId)) {
            if (excludeCourseMedicineId != null && excludeCourseMedicineId.equals(course.getId())) {
                continue;
            }
            addMedicine(byName, course.getMedicine().getId());
        }
        return List.copyOf(byName.values());
    }

    private void addMedicine(Map<String, SubstanceRef> byName, UUID medicineId) {
        substanceInMedicineRepository.findByMedicineId(medicineId)
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
