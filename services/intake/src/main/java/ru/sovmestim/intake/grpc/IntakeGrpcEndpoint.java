package ru.sovmestim.intake.grpc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.intake.dto.CourseMedicineRequest;
import ru.sovmestim.intake.service.IntakeSyncService;
import ru.sovmestim.intake.service.MedicationService;
import ru.sovmestim.proto.intake.v1.ActiveCourseIdsRequest;
import ru.sovmestim.proto.intake.v1.ActiveSubstances;
import ru.sovmestim.proto.intake.v1.AddMedicationRequest;
import ru.sovmestim.proto.intake.v1.ChangesSinceRequest;
import ru.sovmestim.proto.intake.v1.DeleteMedicationRequest;
import ru.sovmestim.proto.intake.v1.IntakeServiceGrpc;
import ru.sovmestim.proto.intake.v1.ListMedicationsRequest;
import ru.sovmestim.proto.intake.v1.Medication;
import ru.sovmestim.proto.intake.v1.MedicationChanges;
import ru.sovmestim.proto.intake.v1.MedicationList;
import ru.sovmestim.proto.intake.v1.MedicineIds;
import ru.sovmestim.proto.intake.v1.MedicineIdsForCoursesRequest;
import ru.sovmestim.proto.intake.v1.UpsertMedicationRequest;
import ru.sovmestim.proto.intake.v1.UuidList;

/**
 * gRPC surface of the intake service. Calls with a client timestamp use the sync path; calls
 * without one use the interactive path.
 */
@Component
@RequiredArgsConstructor
public class IntakeGrpcEndpoint extends IntakeServiceGrpc.IntakeServiceImplBase {

    private static final String ID_REQUIRED = "id is required";

    private final MedicationService medicationService;
    private final IntakeSyncService syncService;

    @Override
    public void listActiveMedications(ListMedicationsRequest request, StreamObserver<MedicationList> observer) {
        MedicationList.Builder list = MedicationList.newBuilder();
        medicationService
                .list(UUID.fromString(request.getUserId()))
                .forEach(view -> list.addMedications(IntakeProtoMapper.toProto(view)));
        observer.onNext(list.build());
        observer.onCompleted();
    }

    @Override
    public void addMedication(AddMedicationRequest request, StreamObserver<Medication> observer) {
        CourseMedicineRequest body = new CourseMedicineRequest(
                ProtoIds.toUuid(request.getMedicineId()),
                request.getDrugName(),
                request.getDosage(),
                request.getFrequency(),
                date(request.getStartDate()));
        observer.onNext(IntakeProtoMapper.toProto(
                medicationService.add(UUID.fromString(request.getUserId()), body)));
        observer.onCompleted();
    }

    @Override
    public void deleteMedication(
            DeleteMedicationRequest request, StreamObserver<ru.sovmestim.proto.intake.v1.ChangeResult> observer) {
        UUID userId = UUID.fromString(request.getUserId());
        UUID id = ProtoIds.toUuid(request.getId());
        ChangeResult result = request.hasClientUpdatedAt()
                ? syncService.delete(userId, id)
                : interactiveDelete(userId, id);
        observer.onNext(IntakeProtoMapper.toProto(result));
        observer.onCompleted();
    }

    @Override
    public void upsertMedication(
            UpsertMedicationRequest request, StreamObserver<ru.sovmestim.proto.intake.v1.UpsertResult> observer) {
        var result = syncService.upsert(
                UUID.fromString(request.getUserId()),
                ProtoIds.toUuid(request.getId()),
                ProtoTime.toInstant(request.getClientUpdatedAt()),
                ProtoIds.toUuid(request.getMedicineId()),
                request.getDrugName(),
                request.getDosage(),
                request.getFrequency(),
                date(request.getStartDate()),
                request.getStatus());
        observer.onNext(IntakeProtoMapper.toProto(result));
        observer.onCompleted();
    }

    @Override
    public void changesSince(ChangesSinceRequest request, StreamObserver<MedicationChanges> observer) {
        var delta = syncService.changesSince(
                UUID.fromString(request.getUserId()), ProtoTime.toInstant(request.getCursor()));
        MedicationChanges response = MedicationChanges.newBuilder()
                .addAllMedications(delta.medications().stream().map(IntakeProtoMapper::toProto).toList())
                .setNextCursor(ProtoTime.toProto(delta.nextCursor()))
                .build();
        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void activeCourseIds(ActiveCourseIdsRequest request, StreamObserver<UuidList> observer) {
        List<String> ids = medicationService.activeCourseIds(UUID.fromString(request.getUserId())).stream()
                .map(UUID::toString)
                .toList();
        observer.onNext(UuidList.newBuilder().addAllIds(ids).build());
        observer.onCompleted();
    }

    @Override
    public void medicineIdsForCourses(
            MedicineIdsForCoursesRequest request, StreamObserver<MedicineIds> observer) {
        Map<UUID, UUID> byCourse = medicationService.medicineIdsForCourses(
                UUID.fromString(request.getUserId()),
                request.getCourseIdsList().stream().map(UUID::fromString).toList());
        MedicineIds.Builder response = MedicineIds.newBuilder();
        byCourse.forEach((course, medicine) -> response.putByCourse(course.toString(), medicine.toString()));
        observer.onNext(response.build());
        observer.onCompleted();
    }

    @Override
    public void listActiveSubstances(ListMedicationsRequest request, StreamObserver<ActiveSubstances> observer) {
        ActiveSubstances.Builder response = ActiveSubstances.newBuilder();
        medicationService.activeSubstances(UUID.fromString(request.getUserId())).forEach(ref -> response.addRefs(
                ru.sovmestim.proto.intake.v1.TakenSubstanceRef.newBuilder()
                        .setMedicineId(ProtoIds.toText(ref.medicineId()))
                        .setActiveSubstanceId(ProtoIds.toText(ref.activeSubstanceId()))
                        .build()));
        observer.onNext(response.build());
        observer.onCompleted();
    }

    private ChangeResult interactiveDelete(UUID userId, UUID id) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        medicationService.delete(userId, id);
        return ChangeResult.applied(id, null);
    }

    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
