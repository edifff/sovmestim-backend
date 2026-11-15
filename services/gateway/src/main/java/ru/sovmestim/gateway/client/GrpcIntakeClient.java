package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.gateway.dto.CourseMedicineRequest;
import ru.sovmestim.gateway.dto.CourseMedicineView;
import ru.sovmestim.proto.intake.v1.ActiveCourseIdsRequest;
import ru.sovmestim.proto.intake.v1.AddMedicationRequest;
import ru.sovmestim.proto.intake.v1.ChangesSinceRequest;
import ru.sovmestim.proto.intake.v1.DeleteMedicationRequest;
import ru.sovmestim.proto.intake.v1.IntakeServiceGrpc;
import ru.sovmestim.proto.intake.v1.ListMedicationsRequest;
import ru.sovmestim.proto.intake.v1.MedicineIdsForCoursesRequest;
import ru.sovmestim.proto.intake.v1.UpsertMedicationRequest;

/**
 * gRPC intake client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcIntakeClient implements IntakeClient {

    private static final long DEADLINE_SECONDS = 3;

    private final GrpcClientFactory channels;

    @Override
    public List<CourseMedicineView> activeMedications(UUID userId) {
        return stub()
                .listActiveMedications(ListMedicationsRequest.newBuilder()
                        .setUserId(userId.toString())
                        .build())
                .getMedicationsList()
                .stream()
                .map(GatewayProtoMapper::courseView)
                .toList();
    }

    @Override
    public CourseMedicineView addMedication(UUID userId, CourseMedicineRequest request) {
        AddMedicationRequest.Builder builder = AddMedicationRequest.newBuilder()
                .setUserId(userId.toString())
                .setMedicineId(ProtoIds.toText(request.medicineId()))
                .setDrugName(GatewayProtoMapper.text(request.drugName()))
                .setDosage(GatewayProtoMapper.text(request.dosage()))
                .setFrequency(GatewayProtoMapper.text(request.frequency()));
        if (request.startDate() != null) {
            builder.setStartDate(request.startDate().toString());
        }
        return GatewayProtoMapper.courseView(stub().addMedication(builder.build()));
    }

    @Override
    public ChangeResult deleteMedication(UUID userId, UUID id, Instant clientUpdatedAt) {
        DeleteMedicationRequest.Builder builder = DeleteMedicationRequest.newBuilder()
                .setUserId(userId.toString())
                .setId(ProtoIds.toText(id));
        if (clientUpdatedAt != null) {
            builder.setClientUpdatedAt(ProtoTime.toProto(clientUpdatedAt));
        }
        return GatewayProtoMapper.intakeChange(stub().deleteMedication(builder.build()));
    }

    @Override
    public UpsertResult upsertMedication(
            UUID userId,
            UUID id,
            Instant clientUpdatedAt,
            UUID medicineId,
            String drugName,
            String dosage,
            String frequency,
            LocalDate startDate,
            String status) {
        UpsertMedicationRequest.Builder builder = UpsertMedicationRequest.newBuilder()
                .setUserId(userId.toString())
                .setId(ProtoIds.toText(id))
                .setClientUpdatedAt(ProtoTime.toProto(clientUpdatedAt))
                .setMedicineId(ProtoIds.toText(medicineId))
                .setDrugName(GatewayProtoMapper.text(drugName))
                .setDosage(GatewayProtoMapper.text(dosage))
                .setFrequency(GatewayProtoMapper.text(frequency))
                .setStatus(GatewayProtoMapper.text(status));
        if (startDate != null) {
            builder.setStartDate(startDate.toString());
        }
        var response = stub().upsertMedication(builder.build());
        return new UpsertResult(
                GatewayProtoMapper.intakeChange(response.getResult()),
                ProtoIds.toUuid(response.getCourseIdForCheck()));
    }

    @Override
    public List<UUID> activeCourseIds(UUID userId) {
        return stub()
                .activeCourseIds(ActiveCourseIdsRequest.newBuilder()
                        .setUserId(userId.toString())
                        .build())
                .getIdsList()
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    @Override
    public Map<UUID, UUID> medicineIdsForCourses(UUID userId, Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        MedicineIdsForCoursesRequest request = MedicineIdsForCoursesRequest.newBuilder()
                .setUserId(userId.toString())
                .addAllCourseIds(courseIds.stream().map(UUID::toString).toList())
                .build();
        Map<UUID, UUID> byCourse = new LinkedHashMap<>();
        stub().medicineIdsForCourses(request).getByCourseMap()
                .forEach((course, medicine) -> byCourse.put(UUID.fromString(course), UUID.fromString(medicine)));
        return byCourse;
    }

    @Override
    public MedicationDelta changesSince(UUID userId, Instant cursor) {
        ChangesSinceRequest.Builder builder = ChangesSinceRequest.newBuilder().setUserId(userId.toString());
        if (cursor != null) {
            builder.setCursor(ProtoTime.toProto(cursor));
        }
        var response = stub().changesSince(builder.build());
        return new MedicationDelta(
                response.getMedicationsList().stream().map(GatewayProtoMapper::medicationChange).toList(),
                ProtoTime.toInstant(response.getNextCursor()));
    }

    private IntakeServiceGrpc.IntakeServiceBlockingStub stub() {
        return IntakeServiceGrpc.newBlockingStub(channels.channel("intake"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
