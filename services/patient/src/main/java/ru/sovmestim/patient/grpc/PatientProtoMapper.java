package ru.sovmestim.patient.grpc;

import java.time.LocalDate;

import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.dto.AllergyView;
import ru.sovmestim.patient.dto.ConditionView;
import ru.sovmestim.proto.patient.v1.Allergy;
import ru.sovmestim.proto.patient.v1.Condition;

/**
 * Maps patient entities and views to the generated contract types.
 */
final class PatientProtoMapper {

    private PatientProtoMapper() {
    }

    static ru.sovmestim.proto.patient.v1.ChangeResult toProto(ChangeResult result) {
        return ru.sovmestim.proto.patient.v1.ChangeResult.newBuilder()
                .setId(ProtoIds.toText(result.id()))
                .setStatus(result.status())
                .setMessage(text(result.message()))
                .setUpdatedAt(ProtoTime.toProto(result.updatedAt()))
                .build();
    }

    static Allergy toProto(AllergyView view) {
        return Allergy.newBuilder()
                .setId(view.id().toString())
                .setName(view.name())
                .setCode(text(view.code()))
                .setSeverity(text(view.severity()))
                .setSymptoms(text(view.symptoms()))
                .setReason(text(view.reason()))
                .setUpdatedAt(ProtoTime.toProto(view.updatedAt()))
                .build();
    }

    static Condition toProto(ConditionView view) {
        Condition.Builder condition = Condition.newBuilder()
                .setId(view.id().toString())
                .setName(view.name())
                .setMkbCode(text(view.mkbCode()))
                .setStatus(text(view.status()))
                .setNote(text(view.note()))
                .setUpdatedAt(ProtoTime.toProto(view.updatedAt()));
        if (view.diagnosisDate() != null) {
            condition.setDiagnosisDate(view.diagnosisDate().toString());
        }
        return condition.build();
    }

    static Allergy toProto(AllergyUser entity) {
        String severity =
                entity.getSeverityReaction() != null ? entity.getSeverityReaction().getName() : null;
        return Allergy.newBuilder()
                .setId(entity.getId().toString())
                .setName(entity.getAllergy().getName())
                .setCode(text(entity.getAllergy().getCode()))
                .setSeverity(text(severity))
                .setSymptoms(text(entity.getSymptoms()))
                .setReason(text(entity.getReason()))
                .setUpdatedAt(ProtoTime.toProto(entity.getUpdatedAt()))
                .setDeleted(entity.isDeleted())
                .build();
    }

    static Condition toProto(ChronicDiseaseUser entity) {
        String mkbCode =
                entity.getChronicDisease().getMkb() != null ? entity.getChronicDisease().getMkb().getCode() : null;
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        Condition.Builder condition = Condition.newBuilder()
                .setId(entity.getId().toString())
                .setName(entity.getChronicDisease().getName())
                .setMkbCode(text(mkbCode))
                .setStatus(text(status))
                .setNote(text(entity.getNote()))
                .setUpdatedAt(ProtoTime.toProto(entity.getUpdatedAt()))
                .setDeleted(entity.isDeleted());
        LocalDate diagnosisDate = entity.getDiagnosisDate();
        if (diagnosisDate != null) {
            condition.setDiagnosisDate(diagnosisDate.toString());
        }
        return condition.build();
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
