package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.gateway.dto.AllergyRequest;
import ru.sovmestim.gateway.dto.AllergyView;
import ru.sovmestim.gateway.dto.ConditionRequest;
import ru.sovmestim.gateway.dto.ConditionView;
import ru.sovmestim.proto.patient.v1.ChangesSinceRequest;
import ru.sovmestim.proto.patient.v1.DeleteAllergyRequest;
import ru.sovmestim.proto.patient.v1.DeleteConditionRequest;
import ru.sovmestim.proto.patient.v1.GetAllergiesRequest;
import ru.sovmestim.proto.patient.v1.GetConditionsRequest;
import ru.sovmestim.proto.patient.v1.PatientServiceGrpc;
import ru.sovmestim.proto.patient.v1.UpsertAllergyRequest;
import ru.sovmestim.proto.patient.v1.UpsertConditionRequest;

/**
 * gRPC patient client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcPatientClient implements PatientClient {

    private static final long DEADLINE_SECONDS = 3;

    private final GrpcClientFactory channels;

    @Override
    public List<AllergyView> allergies(UUID userId) {
        return stub()
                .getAllergies(GetAllergiesRequest.newBuilder().setUserId(userId.toString()).build())
                .getAllergiesList()
                .stream()
                .map(GatewayProtoMapper::allergyView)
                .toList();
    }

    @Override
    public List<ConditionView> conditions(UUID userId) {
        return stub()
                .getConditions(GetConditionsRequest.newBuilder().setUserId(userId.toString()).build())
                .getConditionsList()
                .stream()
                .map(GatewayProtoMapper::conditionView)
                .toList();
    }

    @Override
    public ChangeResult upsertAllergy(UUID userId, UUID id, Instant clientUpdatedAt, AllergyRequest request) {
        UpsertAllergyRequest.Builder builder = UpsertAllergyRequest.newBuilder()
                .setUserId(userId.toString())
                .setId(ProtoIds.toText(id))
                .setName(request.name())
                .setSeverity(GatewayProtoMapper.text(request.severity()))
                .setSymptoms(GatewayProtoMapper.text(request.symptoms()))
                .setReason(GatewayProtoMapper.text(request.reason()));
        if (clientUpdatedAt != null) {
            builder.setClientUpdatedAt(ProtoTime.toProto(clientUpdatedAt));
        }
        return GatewayProtoMapper.patientChange(stub().upsertAllergy(builder.build()));
    }

    @Override
    public ChangeResult deleteAllergy(UUID userId, UUID id, Instant clientUpdatedAt) {
        DeleteAllergyRequest.Builder builder = DeleteAllergyRequest.newBuilder()
                .setUserId(userId.toString())
                .setId(ProtoIds.toText(id));
        if (clientUpdatedAt != null) {
            builder.setClientUpdatedAt(ProtoTime.toProto(clientUpdatedAt));
        }
        return GatewayProtoMapper.patientChange(stub().deleteAllergy(builder.build()));
    }

    @Override
    public ChangeResult upsertCondition(
            UUID userId, UUID id, Instant clientUpdatedAt, ConditionRequest request) {
        UpsertConditionRequest.Builder builder = UpsertConditionRequest.newBuilder()
                .setUserId(userId.toString())
                .setId(ProtoIds.toText(id))
                .setName(request.name())
                .setMkbCode(GatewayProtoMapper.text(request.mkbCode()))
                .setStatus(GatewayProtoMapper.text(request.status()))
                .setNote(GatewayProtoMapper.text(request.note()));
        if (request.diagnosisDate() != null) {
            builder.setDiagnosisDate(request.diagnosisDate().toString());
        }
        if (clientUpdatedAt != null) {
            builder.setClientUpdatedAt(ProtoTime.toProto(clientUpdatedAt));
        }
        return GatewayProtoMapper.patientChange(stub().upsertCondition(builder.build()));
    }

    @Override
    public ChangeResult deleteCondition(UUID userId, UUID id, Instant clientUpdatedAt) {
        DeleteConditionRequest.Builder builder = DeleteConditionRequest.newBuilder()
                .setUserId(userId.toString())
                .setId(ProtoIds.toText(id));
        if (clientUpdatedAt != null) {
            builder.setClientUpdatedAt(ProtoTime.toProto(clientUpdatedAt));
        }
        return GatewayProtoMapper.patientChange(stub().deleteCondition(builder.build()));
    }

    @Override
    public PatientDelta changesSince(UUID userId, Instant cursor) {
        ChangesSinceRequest.Builder builder = ChangesSinceRequest.newBuilder().setUserId(userId.toString());
        if (cursor != null) {
            builder.setCursor(ProtoTime.toProto(cursor));
        }
        var response = stub().changesSince(builder.build());
        return new PatientDelta(
                response.getAllergiesList().stream().map(GatewayProtoMapper::allergyChange).toList(),
                response.getConditionsList().stream().map(GatewayProtoMapper::conditionChange).toList(),
                ProtoTime.toInstant(response.getNextCursor()));
    }

    private PatientServiceGrpc.PatientServiceBlockingStub stub() {
        return PatientServiceGrpc.newBlockingStub(channels.channel("patient"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
