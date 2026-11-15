package ru.sovmestim.advice.client;

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
import ru.sovmestim.proto.intake.v1.ActiveCourseIdsRequest;
import ru.sovmestim.proto.intake.v1.IntakeServiceGrpc;
import ru.sovmestim.proto.intake.v1.ListMedicationsRequest;
import ru.sovmestim.proto.intake.v1.MedicineIdsForCoursesRequest;

/**
 * gRPC intake client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcIntakeClient implements IntakeClient {

    private static final long DEADLINE_SECONDS = 2;

    private final GrpcClientFactory channels;

    @Override
    public List<MedicationInfo> activeMedications(UUID userId) {
        return stub()
                .listActiveMedications(ListMedicationsRequest.newBuilder()
                        .setUserId(userId.toString())
                        .build())
                .getMedicationsList()
                .stream()
                .map(medication -> new MedicationInfo(
                        UUID.fromString(medication.getId()),
                        ProtoIds.toUuid(medication.getMedicineId()),
                        medication.getMedicineName()))
                .toList();
    }

    @Override
    public List<TakenSubstanceRef> activeSubstances(UUID userId) {
        return stub()
                .listActiveSubstances(ListMedicationsRequest.newBuilder()
                        .setUserId(userId.toString())
                        .build())
                .getRefsList()
                .stream()
                .map(ref -> new TakenSubstanceRef(
                        ProtoIds.toUuid(ref.getMedicineId()), ProtoIds.toUuid(ref.getActiveSubstanceId())))
                .toList();
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

    private IntakeServiceGrpc.IntakeServiceBlockingStub stub() {
        return IntakeServiceGrpc.newBlockingStub(channels.channel("intake"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
