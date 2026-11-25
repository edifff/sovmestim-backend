package ru.sovmestim.intake.grpc;

import java.time.Instant;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.sovmestim.intake.dto.CourseMedicineView;

/**
 * Verifies the gRPC mapping of active medication courses.
 */
class IntakeProtoMapperTest {

    private static final String MEDICINE = "Варфарин";

    @Test
    void activeMedicationCarriesCatalogMedicineId() {
        UUID medicineId = UUID.randomUUID();
        CourseMedicineView view = new CourseMedicineView(
                UUID.randomUUID(),
                medicineId,
                MEDICINE,
                MEDICINE,
                "2.5 мг",
                "1 раз в день",
                null,
                null,
                Instant.now());

        var proto = IntakeProtoMapper.toProto(view);

        Assertions.assertThat(proto.getMedicineId()).isEqualTo(medicineId.toString());
    }
}
