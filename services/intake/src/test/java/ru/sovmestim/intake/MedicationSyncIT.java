package ru.sovmestim.intake;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.intake.client.CatalogClient;
import ru.sovmestim.intake.client.ResolvedMedicine;
import ru.sovmestim.intake.dto.CourseMedicineRequest;
import ru.sovmestim.intake.service.IntakeSyncService;
import ru.sovmestim.intake.service.MedicationService;

/**
 * Intake service against a real PostgreSQL. The catalog is replaced by a deterministic fake, so the
 * test does not need a second running service.
 */
@SpringBootTest(properties = "sovmestim.grpc.port=0")
@Import(MedicationSyncIT.FakeCatalog.class)
class MedicationSyncIT {

    private static final String WARFARIN = "Варфарин";
    private static final String WARFARIN_QUERY = "варфарин";
    private static final String UNKNOWN = "Неизвестное лекарство";
    private static final String DOSAGE = "5 mg";
    private static final String DOSAGE_TEN = "10 mg";
    private static final String DAILY = "1/day";
    private static final String TWICE_DAILY = "2/day";

    private static final UUID MEDICINE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String DB_USER = "sovmestim";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sovmestim_intake")
            .withUsername(DB_USER)
            .withPassword(DB_USER);

    static {
        POSTGRES.start();
    }

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private IntakeSyncService syncService;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void addKeepsTheMedicineNameSnapshot() {
        UUID userId = UUID.randomUUID();

        var view = medicationService.add(
                userId, new CourseMedicineRequest(MEDICINE_ID, null, DOSAGE, TWICE_DAILY, LocalDate.now()));

        Assertions.assertThat(view.medicineName()).isEqualTo(WARFARIN);
        Assertions.assertThat(medicationService.list(userId)).singleElement()
                .satisfies(stored -> Assertions.assertThat(stored.brand()).isEqualTo(WARFARIN));
    }

    @Test
    void syncUpsertConflictsOnAnOlderClientChange() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        syncService.upsert(
                userId, courseId, Instant.now().plusSeconds(60), MEDICINE_ID, null,
                DOSAGE, DAILY, LocalDate.now(), null);

        var result = syncService.upsert(
                userId, courseId, Instant.EPOCH, MEDICINE_ID, null,
                DOSAGE_TEN, TWICE_DAILY, LocalDate.now(), null);

        Assertions.assertThat(result.result().status()).isEqualTo(ChangeResult.CONFLICT);
    }

    @Test
    void unresolvedNameBecomesALocalDraft() {
        UUID userId = UUID.randomUUID();

        var view = medicationService.add(
                userId, new CourseMedicineRequest(null, UNKNOWN, null, null, null));

        Assertions.assertThat(view.medicineName()).isEqualTo(UNKNOWN);
        Assertions.assertThat(medicationService.medicineIdsForCourses(userId, java.util.List.of(view.id()))).isEmpty();
    }

    /**
     * Deterministic catalog stub: one known medicine, everything else unresolved.
     */
    @TestConfiguration
    static class FakeCatalog {

        @Bean
        @Primary
        CatalogClient catalogClient() {
            return new CatalogClient() {
                @Override
                public ResolvedMedicine medicine(UUID id) {
                    return new ResolvedMedicine(MEDICINE_ID, WARFARIN, WARFARIN);
                }

                @Override
                public Optional<ResolvedMedicine> resolve(String name) {
                    return WARFARIN_QUERY.equalsIgnoreCase(name)
                            ? Optional.of(new ResolvedMedicine(MEDICINE_ID, WARFARIN, WARFARIN))
                            : Optional.empty();
                }
            };
        }
    }
}
