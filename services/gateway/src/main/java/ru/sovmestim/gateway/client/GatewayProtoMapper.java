package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.google.protobuf.Timestamp;

import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.gateway.dto.AdviceCheckResponse;
import ru.sovmestim.gateway.dto.AdviceFinding;
import ru.sovmestim.gateway.dto.AdviceKind;
import ru.sovmestim.gateway.dto.AdviceLevel;
import ru.sovmestim.gateway.dto.AdviceResult;
import ru.sovmestim.gateway.dto.AdviceSourceRef;
import ru.sovmestim.gateway.dto.AdviceStatus;
import ru.sovmestim.gateway.dto.AllergyView;
import ru.sovmestim.gateway.dto.ConditionView;
import ru.sovmestim.gateway.dto.CourseMedicineView;
import ru.sovmestim.gateway.dto.MedicineView;
import ru.sovmestim.gateway.dto.ResolvedDrug;
import ru.sovmestim.gateway.dto.SubstanceDoseView;
import ru.sovmestim.gateway.dto.SubstanceRef;
import ru.sovmestim.gateway.dto.SubstanceView;
import ru.sovmestim.gateway.sync.dto.AdviceDelivery;
import ru.sovmestim.gateway.sync.dto.AllergyChange;
import ru.sovmestim.gateway.sync.dto.ConditionChange;
import ru.sovmestim.gateway.sync.dto.MedicationChange;

/**
 * Converts generated contract messages into the REST view models of the gateway. All mapping lives
 * in one place so the controllers and the sync orchestrator stay free of protobuf details.
 */
final class GatewayProtoMapper {

    private GatewayProtoMapper() {
    }

    static String text(String value) {
        return value != null ? value : "";
    }

    static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static UUID uuid(String value) {
        return ProtoIds.toUuid(value);
    }

    static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    static Instant instant(Timestamp value) {
        return ProtoTime.toInstant(value);
    }

    static SubstanceRef substanceRef(ru.sovmestim.proto.catalog.v1.Substance substance) {
        return new SubstanceRef(
                ProtoIds.toUuid(substance.getId()),
                substance.getName(),
                textOrNull(substance.getAtcCode()),
                textOrNull(substance.getAtcName()),
                textOrNull(substance.getPhg()));
    }

    static SubstanceView substanceView(ru.sovmestim.proto.catalog.v1.Substance substance) {
        return new SubstanceView(
                UUID.fromString(substance.getId()),
                substance.getName(),
                textOrNull(substance.getAtcCode()),
                textOrNull(substance.getAtcName()),
                textOrNull(substance.getDescription()));
    }

    static MedicineView medicineView(ru.sovmestim.proto.catalog.v1.Medicine medicine) {
        List<SubstanceDoseView> substances = medicine.getSubstancesList().stream()
                .map(substance -> new SubstanceDoseView(
                        UUID.fromString(substance.getId()), substance.getName(), textOrNull(substance.getDosage())))
                .toList();
        return new MedicineView(
                UUID.fromString(medicine.getId()),
                medicine.getName(),
                textOrNull(medicine.getBrand()),
                textOrNull(medicine.getFormRelease()),
                substances);
    }

    static ResolvedDrug resolvedDrug(ru.sovmestim.proto.catalog.v1.ResolveDrugResponse response) {
        List<SubstanceRef> substances =
                response.getSubstancesList().stream().map(GatewayProtoMapper::substanceRef).toList();
        return new ResolvedDrug(ProtoIds.toUuid(response.getMedicineId()), response.getName(), substances);
    }

    static AllergyView allergyView(ru.sovmestim.proto.patient.v1.Allergy allergy) {
        return new AllergyView(
                UUID.fromString(allergy.getId()),
                allergy.getName(),
                textOrNull(allergy.getCode()),
                textOrNull(allergy.getSeverity()),
                textOrNull(allergy.getSymptoms()),
                textOrNull(allergy.getReason()),
                instant(allergy.getUpdatedAt()));
    }

    static ConditionView conditionView(ru.sovmestim.proto.patient.v1.Condition condition) {
        return new ConditionView(
                UUID.fromString(condition.getId()),
                condition.getName(),
                textOrNull(condition.getMkbCode()),
                textOrNull(condition.getStatus()),
                date(condition.getDiagnosisDate()),
                textOrNull(condition.getNote()),
                instant(condition.getUpdatedAt()));
    }

    static AllergyChange allergyChange(ru.sovmestim.proto.patient.v1.Allergy allergy) {
        return new AllergyChange(
                UUID.fromString(allergy.getId()),
                instant(allergy.getUpdatedAt()),
                allergy.getDeleted(),
                allergy.getName(),
                textOrNull(allergy.getSeverity()),
                textOrNull(allergy.getSymptoms()),
                textOrNull(allergy.getReason()));
    }

    static ConditionChange conditionChange(ru.sovmestim.proto.patient.v1.Condition condition) {
        return new ConditionChange(
                UUID.fromString(condition.getId()),
                instant(condition.getUpdatedAt()),
                condition.getDeleted(),
                condition.getName(),
                textOrNull(condition.getMkbCode()),
                textOrNull(condition.getStatus()),
                date(condition.getDiagnosisDate()),
                textOrNull(condition.getNote()));
    }

    static CourseMedicineView courseView(ru.sovmestim.proto.intake.v1.Medication medication) {
        return new CourseMedicineView(
                UUID.fromString(medication.getId()),
                ProtoIds.toUuid(medication.getMedicineId()),
                medication.getMedicineName(),
                textOrNull(medication.getBrand()),
                textOrNull(medication.getDosage()),
                textOrNull(medication.getFrequency()),
                date(medication.getStartDate()),
                textOrNull(medication.getStatus()),
                instant(medication.getUpdatedAt()));
    }

    static MedicationChange medicationChange(ru.sovmestim.proto.intake.v1.Medication medication) {
        return new MedicationChange(
                UUID.fromString(medication.getId()),
                instant(medication.getUpdatedAt()),
                medication.getDeleted(),
                ProtoIds.toUuid(medication.getMedicineId()),
                medication.getMedicineName(),
                textOrNull(medication.getDosage()),
                textOrNull(medication.getFrequency()),
                date(medication.getStartDate()),
                textOrNull(medication.getStatus()));
    }

    static ru.sovmestim.common.sync.ChangeResult patientChange(
            ru.sovmestim.proto.patient.v1.ChangeResult result) {
        return new ru.sovmestim.common.sync.ChangeResult(
                ProtoIds.toUuid(result.getId()), result.getStatus(), textOrNull(result.getMessage()),
                instant(result.getUpdatedAt()));
    }

    static ru.sovmestim.common.sync.ChangeResult intakeChange(
            ru.sovmestim.proto.intake.v1.ChangeResult result) {
        return new ru.sovmestim.common.sync.ChangeResult(
                ProtoIds.toUuid(result.getId()), result.getStatus(), textOrNull(result.getMessage()),
                instant(result.getUpdatedAt()));
    }

    static AdviceCheckResponse adviceCheck(ru.sovmestim.proto.advice.v1.AdviceResponse response) {
        return new AdviceCheckResponse(UUID.fromString(response.getAdviceId()), adviceResult(response.getResult()));
    }

    static AdviceResult adviceResult(ru.sovmestim.proto.advice.v1.AdviceResult result) {
        List<AdviceFinding> findings = result.getFindingsList().stream()
                .map(GatewayProtoMapper::finding)
                .toList();
        return new AdviceResult(
                status(result.getStatus()),
                level(result.getLevel()),
                findings,
                result.getCheckedSubstances(),
                textOrNull(result.getRulesVersion()),
                textOrNull(result.getMappingVersion()),
                textOrNull(result.getCatalogVersion()),
                instant(result.getCheckedAt()),
                result.getNotesList());
    }

    static AdviceDelivery adviceDelivery(ru.sovmestim.proto.advice.v1.AdviceDelivery delivery) {
        return new AdviceDelivery(
                delivery.getAdviceId(),
                delivery.getDrugName(),
                instant(delivery.getCreatedAt()),
                adviceResult(delivery.getResult()));
    }

    private static AdviceFinding finding(ru.sovmestim.proto.advice.v1.AdviceFinding finding) {
        List<AdviceSourceRef> sources = finding.getSourcesList().stream()
                .map(source -> new AdviceSourceRef(
                        textOrNull(source.getKind()),
                        textOrNull(source.getName()),
                        date(source.getDate()),
                        textOrNull(source.getNote())))
                .toList();
        return new AdviceFinding(
                kind(finding.getKind()),
                level(finding.getLevel()),
                textOrNull(finding.getTitle()),
                textOrNull(finding.getText()),
                finding.getSubstancesList(),
                sources);
    }

    private static AdviceStatus status(String value) {
        return value == null || value.isBlank() ? null : AdviceStatus.valueOf(value);
    }

    private static AdviceLevel level(String value) {
        return value == null || value.isBlank() ? null : AdviceLevel.valueOf(value);
    }

    private static AdviceKind kind(String value) {
        return value == null || value.isBlank() ? null : AdviceKind.valueOf(value);
    }
}
