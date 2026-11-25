package ru.sovmestim.advice.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.sovmestim.advice.client.CatalogClient;
import ru.sovmestim.advice.client.IntakeClient;
import ru.sovmestim.advice.client.MedicationInfo;
import ru.sovmestim.advice.client.MedicineInfo;
import ru.sovmestim.advice.client.PatientClient;
import ru.sovmestim.advice.client.ResolvedDrug;
import ru.sovmestim.advice.client.TakenSubstanceRef;
import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.PatientCondition;
import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Tests the snapshot assembly over the client interfaces: current substances, allergies and
 * conditions are merged and deduplicated by normalized name.
 */
class PatientSnapshotServiceTest {

    private static final String WARFARIN = "Варфарин";
    private static final String PENICILLIN = "Пенициллин";
    private static final String HYPERTENSION = "Гипертония";
    private static final String ICD_HYPERTENSION = "I10";

    private static final UUID USER = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID MEDICINE = UUID.randomUUID();
    private static final UUID SUBSTANCE = UUID.randomUUID();

    private final PatientSnapshotService service = new PatientSnapshotService(
            new FakePatientClient(), new FakeIntakeClient(), new FakeCatalogClient());

    @Test
    void mergesMedicationSubstancesAndDeduplicatesByName() {
        var snapshot = service.snapshot(USER);

        Assertions.assertThat(snapshot.currentSubstances()).singleElement()
                .satisfies(substance -> Assertions.assertThat(substance.name()).isEqualTo(WARFARIN));
        Assertions.assertThat(snapshot.allergies()).singleElement()
                .satisfies(allergy -> Assertions.assertThat(allergy.name()).isEqualTo(PENICILLIN));
        Assertions.assertThat(snapshot.conditions()).singleElement()
                .satisfies(condition -> Assertions.assertThat(condition.mkbCode()).isEqualTo(ICD_HYPERTENSION));
    }

    @Test
    void excludesTheCourseBeingCheckedFromItsOwnSnapshot() {
        var snapshot = service.snapshot(USER, COURSE);

        Assertions.assertThat(snapshot.currentSubstances()).isEmpty();
    }

    /**
     * Patient stub with one allergy and one condition.
     */
    private static final class FakePatientClient implements PatientClient {

        @Override
        public List<PatientAllergy> allergies(UUID userId) {
            return List.of(new PatientAllergy(PENICILLIN, "J01", "HIGH"));
        }

        @Override
        public List<PatientCondition> conditions(UUID userId) {
            return List.of(new PatientCondition(HYPERTENSION, ICD_HYPERTENSION));
        }
    }

    /**
     * Intake stub with one active course.
     */
    private static final class FakeIntakeClient implements IntakeClient {

        @Override
        public List<MedicationInfo> activeMedications(UUID userId) {
            return List.of(new MedicationInfo(COURSE, MEDICINE, WARFARIN));
        }

        @Override
        public List<TakenSubstanceRef> activeSubstances(UUID userId) {
            return List.of();
        }

        @Override
        public List<UUID> activeCourseIds(UUID userId) {
            return List.of(COURSE);
        }

        @Override
        public Map<UUID, UUID> medicineIdsForCourses(UUID userId, Collection<UUID> courseIds) {
            return Map.of(COURSE, MEDICINE);
        }
    }

    /**
     * Catalog stub returning the same substance twice to exercise deduplication.
     */
    private static final class FakeCatalogClient implements CatalogClient {

        private final SubstanceRef warfarin =
                new SubstanceRef(SUBSTANCE, WARFARIN, "B01AA03", "Anticoagulants", null);

        @Override
        public MedicineInfo medicine(UUID id) {
            return new MedicineInfo(MEDICINE, WARFARIN, List.of(warfarin));
        }

        @Override
        public Optional<ResolvedDrug> resolve(String name) {
            return Optional.of(new ResolvedDrug(MEDICINE, name, List.of(warfarin)));
        }

        @Override
        public List<SubstanceRef> substancesByIds(List<UUID> ids) {
            return ids.isEmpty() ? List.of() : List.of(warfarin);
        }

        @Override
        public List<SubstanceRef> substancesForMedicine(UUID medicineId) {
            return List.of(warfarin);
        }

        @Override
        public List<SubstanceRef> substancesForMedicines(Collection<UUID> medicineIds) {
            return medicineIds.isEmpty() ? List.of() : List.of(warfarin, warfarin);
        }
    }
}
