package ru.sovmestim.patient;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.patient.dto.AllergyRequest;
import ru.sovmestim.patient.service.PatientService;
import ru.sovmestim.patient.service.PatientSyncService;

/**
 * Patient service against a real PostgreSQL: idempotent upserts, last-write-wins and tombstones.
 */
@SpringBootTest(properties = "sovmestim.grpc.port=0")
class PatientSyncIT {

    private static final String PENICILLIN = "Пенициллин";
    private static final String HIGH = "HIGH";
    private static final String LOW = "LOW";
    private static final String RASH = "сыпь";
    private static final String ANTIBIOTIC = "антибиотик";

    private static final String DB_USER = "sovmestim";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sovmestim_patient")
            .withUsername(DB_USER)
            .withPassword(DB_USER);

    static {
        POSTGRES.start();
    }

    @Autowired
    private PatientSyncService syncService;

    @Autowired
    private PatientService patientService;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void upsertIsVisibleInTheProfileAndInChanges() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        ChangeResult applied = syncService.upsertAllergy(
                userId, id, Instant.now().plusSeconds(60), new AllergyRequest(PENICILLIN, HIGH, RASH, ANTIBIOTIC));

        Assertions.assertThat(applied.status()).isEqualTo(ChangeResult.APPLIED);
        Assertions.assertThat(patientService.listAllergies(userId)).singleElement()
                .satisfies(view -> Assertions.assertThat(view.name()).isEqualTo(PENICILLIN));
        Assertions.assertThat(syncService.changesSince(userId, Instant.EPOCH).allergies()).hasSize(1);
    }

    @Test
    void olderClientChangeLosesTheConflictCheck() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        syncService.upsertAllergy(
                userId, id, Instant.now().plusSeconds(60), new AllergyRequest(PENICILLIN, HIGH, null, null));

        ChangeResult conflict = syncService.upsertAllergy(
                userId, id, Instant.EPOCH, new AllergyRequest(PENICILLIN, LOW, null, null));

        Assertions.assertThat(conflict.status()).isEqualTo(ChangeResult.CONFLICT);
    }

    @Test
    void deleteCreatesATombstoneThatIsPulled() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        syncService.upsertAllergy(
                userId, id, Instant.now().plusSeconds(60), new AllergyRequest(PENICILLIN, null, null, null));

        syncService.deleteAllergy(userId, id);

        Assertions.assertThat(syncService.changesSince(userId, Instant.EPOCH).allergies())
                .singleElement()
                .satisfies(allergy -> Assertions.assertThat(allergy.isDeleted()).isTrue());
    }
}
