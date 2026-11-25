package ru.sovmestim.intake.grpc;

import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.dto.CourseMedicineView;
import ru.sovmestim.intake.service.UpsertResult;
import ru.sovmestim.proto.intake.v1.Medication;

/**
 * Maps medication entities and sync outcomes to the generated contract types.
 */
final class IntakeProtoMapper {

    private IntakeProtoMapper() {
    }

    static Medication toProto(CourseMedicineView view) {
        Medication.Builder medication = Medication.newBuilder()
                .setId(view.id().toString())
                .setMedicineId(ProtoIds.toText(view.medicineId()))
                .setMedicineName(view.medicineName())
                .setBrand(text(view.brand()))
                .setDosage(text(view.dosage()))
                .setFrequency(text(view.frequency()))
                .setStatus(text(view.status()))
                .setUpdatedAt(ProtoTime.toProto(view.updatedAt()));
        if (view.startDate() != null) {
            medication.setStartDate(view.startDate().toString());
        }
        return medication.build();
    }

    static Medication toProto(CourseMedicine entity) {
        Medication.Builder medication = Medication.newBuilder()
                .setId(entity.getId().toString())
                .setMedicineId(ProtoIds.toText(entity.getMedicineId()))
                .setMedicineName(entity.getMedicineName())
                .setBrand(text(entity.getBrand()))
                .setDosage(text(entity.getDosage()))
                .setFrequency(text(entity.getFrequency()))
                .setStatus(entity.getStatus() != null ? entity.getStatus().getName() : "")
                .setUpdatedAt(ProtoTime.toProto(entity.getUpdatedAt()))
                .setDeleted(entity.isDeleted());
        if (entity.getStartDate() != null) {
            medication.setStartDate(entity.getStartDate().toString());
        }
        return medication.build();
    }

    static ru.sovmestim.proto.intake.v1.ChangeResult toProto(ChangeResult result) {
        return ru.sovmestim.proto.intake.v1.ChangeResult.newBuilder()
                .setId(ProtoIds.toText(result.id()))
                .setStatus(result.status())
                .setMessage(text(result.message()))
                .setUpdatedAt(ProtoTime.toProto(result.updatedAt()))
                .build();
    }

    static ru.sovmestim.proto.intake.v1.UpsertResult toProto(UpsertResult result) {
        ru.sovmestim.proto.intake.v1.UpsertResult.Builder response =
                ru.sovmestim.proto.intake.v1.UpsertResult.newBuilder().setResult(toProto(result.result()));
        if (result.courseIdForCheck() != null) {
            response.setCourseIdForCheck(result.courseIdForCheck().toString());
        }
        return response.build();
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
