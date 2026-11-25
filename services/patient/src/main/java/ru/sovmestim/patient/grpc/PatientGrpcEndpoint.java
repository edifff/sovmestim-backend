package ru.sovmestim.patient.grpc;

import java.time.LocalDate;
import java.util.UUID;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.patient.dto.AllergyRequest;
import ru.sovmestim.patient.dto.ConditionRequest;
import ru.sovmestim.patient.service.PatientService;
import ru.sovmestim.patient.service.PatientSyncService;
import ru.sovmestim.proto.patient.v1.AllergyList;
import ru.sovmestim.proto.patient.v1.ChangesSinceRequest;
import ru.sovmestim.proto.patient.v1.ConditionList;
import ru.sovmestim.proto.patient.v1.DeleteAllergyRequest;
import ru.sovmestim.proto.patient.v1.DeleteConditionRequest;
import ru.sovmestim.proto.patient.v1.GetAllergiesRequest;
import ru.sovmestim.proto.patient.v1.GetConditionsRequest;
import ru.sovmestim.proto.patient.v1.PatientChanges;
import ru.sovmestim.proto.patient.v1.PatientServiceGrpc;
import ru.sovmestim.proto.patient.v1.UpsertAllergyRequest;
import ru.sovmestim.proto.patient.v1.UpsertConditionRequest;

/**
 * gRPC surface of the patient profile. Calls carrying a client timestamp use the sync path with
 * last-write-wins; calls without one use the interactive path and report missing rows as
 * {@code NOT_FOUND}.
 */
@Component
@RequiredArgsConstructor
public class PatientGrpcEndpoint extends PatientServiceGrpc.PatientServiceImplBase {

    private static final String ID_REQUIRED = "id is required";

    private final PatientService patientService;
    private final PatientSyncService syncService;

    @Override
    public void getAllergies(GetAllergiesRequest request, StreamObserver<AllergyList> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        AllergyList.Builder list = AllergyList.newBuilder();
        patientService.listAllergies(userId).forEach(view -> list.addAllergies(PatientProtoMapper.toProto(view)));
        observer.onNext(list.build());
        observer.onCompleted();
    }

    @Override
    public void getConditions(GetConditionsRequest request, StreamObserver<ConditionList> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        ConditionList.Builder list = ConditionList.newBuilder();
        patientService
                .listConditions(userId)
                .forEach(view -> list.addConditions(PatientProtoMapper.toProto(view)));
        observer.onNext(list.build());
        observer.onCompleted();
    }

    @Override
    public void upsertAllergy(
            UpsertAllergyRequest request, StreamObserver<ru.sovmestim.proto.patient.v1.ChangeResult> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        UUID id = ProtoIds.toUuid(request.getId());
        AllergyRequest body =
                new AllergyRequest(request.getName(), request.getSeverity(), request.getSymptoms(), request.getReason());
        ChangeResult result = request.hasClientUpdatedAt()
                ? syncService.upsertAllergy(userId, id, ProtoTime.toInstant(request.getClientUpdatedAt()), body)
                : interactiveAllergy(userId, id, body);
        observer.onNext(PatientProtoMapper.toProto(result));
        observer.onCompleted();
    }

    @Override
    public void deleteAllergy(
            DeleteAllergyRequest request, StreamObserver<ru.sovmestim.proto.patient.v1.ChangeResult> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        UUID id = ProtoIds.toUuid(request.getId());
        ChangeResult result = request.hasClientUpdatedAt()
                ? syncService.deleteAllergy(userId, id)
                : interactiveDeleteAllergy(userId, id);
        observer.onNext(PatientProtoMapper.toProto(result));
        observer.onCompleted();
    }

    @Override
    public void upsertCondition(
            UpsertConditionRequest request, StreamObserver<ru.sovmestim.proto.patient.v1.ChangeResult> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        UUID id = ProtoIds.toUuid(request.getId());
        ConditionRequest body = new ConditionRequest(
                request.getName(),
                request.getMkbCode(),
                request.getStatus(),
                date(request.getDiagnosisDate()),
                request.getNote());
        ChangeResult result = request.hasClientUpdatedAt()
                ? syncService.upsertCondition(userId, id, ProtoTime.toInstant(request.getClientUpdatedAt()), body)
                : interactiveCondition(userId, id, body);
        observer.onNext(PatientProtoMapper.toProto(result));
        observer.onCompleted();
    }

    @Override
    public void deleteCondition(
            DeleteConditionRequest request, StreamObserver<ru.sovmestim.proto.patient.v1.ChangeResult> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        UUID id = ProtoIds.toUuid(request.getId());
        ChangeResult result = request.hasClientUpdatedAt()
                ? syncService.deleteCondition(userId, id)
                : interactiveDeleteCondition(userId, id);
        observer.onNext(PatientProtoMapper.toProto(result));
        observer.onCompleted();
    }

    @Override
    public void changesSince(ChangesSinceRequest request, StreamObserver<PatientChanges> observer) {
        var changes = syncService.changesSince(
                UUID.fromString(request.getUserId()), ProtoTime.toInstant(request.getCursor()));
        PatientChanges response = PatientChanges.newBuilder()
                .addAllAllergies(changes.allergies().stream().map(PatientProtoMapper::toProto).toList())
                .addAllConditions(changes.conditions().stream().map(PatientProtoMapper::toProto).toList())
                .setNextCursor(ProtoTime.toProto(changes.nextCursor()))
                .build();
        observer.onNext(response);
        observer.onCompleted();
    }

    private ChangeResult interactiveAllergy(UUID userId, UUID id, AllergyRequest body) {
        var view = id == null ? patientService.addAllergy(userId, body) : patientService.updateAllergy(userId, id, body);
        return ChangeResult.applied(view.id(), view.updatedAt());
    }

    private ChangeResult interactiveCondition(UUID userId, UUID id, ConditionRequest body) {
        if (id != null) {
            throw new BadRequestException("Conditions can only be created through the interactive API");
        }
        var view = patientService.addCondition(userId, body);
        return ChangeResult.applied(view.id(), view.updatedAt());
    }

    private ChangeResult interactiveDeleteAllergy(UUID userId, UUID id) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        patientService.deleteAllergy(userId, id);
        return ChangeResult.applied(id, null);
    }

    private ChangeResult interactiveDeleteCondition(UUID userId, UUID id) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        patientService.deleteCondition(userId, id);
        return ChangeResult.applied(id, null);
    }

    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
