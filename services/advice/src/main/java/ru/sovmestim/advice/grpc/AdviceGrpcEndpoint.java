package ru.sovmestim.advice.grpc;

import java.util.UUID;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.dto.AdviceCheckRequest;
import ru.sovmestim.advice.service.AdviceService;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.proto.advice.v1.AdviceChanges;
import ru.sovmestim.proto.advice.v1.AdviceResponse;
import ru.sovmestim.proto.advice.v1.AdviceResponseList;
import ru.sovmestim.proto.advice.v1.AdviceServiceGrpc;
import ru.sovmestim.proto.advice.v1.AdviceSinceRequest;
import ru.sovmestim.proto.advice.v1.CheckRequest;
import ru.sovmestim.proto.advice.v1.RecheckAllRequest;
import ru.sovmestim.proto.advice.v1.RecheckCoursesRequest;

/**
 * gRPC surface of the advice service.
 */
@Component
@RequiredArgsConstructor
public class AdviceGrpcEndpoint extends AdviceServiceGrpc.AdviceServiceImplBase {

    private final AdviceService adviceService;

    @Override
    public void check(CheckRequest request, StreamObserver<AdviceResponse> observer) {
        AdviceCheckRequest body = new AdviceCheckRequest(
                textOrNull(request.getDrugName()),
                ProtoIds.toUuid(request.getMedicineId()),
                request.getSubstanceIdsList().stream().map(UUID::fromString).toList());
        observer.onNext(AdviceProtoMapper.toProto(
                adviceService.check(UUID.fromString(request.getUserId()), body)));
        observer.onCompleted();
    }

    @Override
    public void recheckCourses(RecheckCoursesRequest request, StreamObserver<AdviceResponseList> observer) {
        AdviceResponseList.Builder list = AdviceResponseList.newBuilder();
        adviceService
                .recheckCourses(
                        UUID.fromString(request.getUserId()),
                        request.getCourseIdsList().stream().map(UUID::fromString).toList())
                .forEach(response -> list.addResponses(AdviceProtoMapper.toProto(response)));
        observer.onNext(list.build());
        observer.onCompleted();
    }

    @Override
    public void recheckAll(RecheckAllRequest request, StreamObserver<AdviceResponseList> observer) {
        AdviceResponseList.Builder list = AdviceResponseList.newBuilder();
        adviceService
                .recheckAll(UUID.fromString(request.getUserId()))
                .forEach(response -> list.addResponses(AdviceProtoMapper.toProto(response)));
        observer.onNext(list.build());
        observer.onCompleted();
    }

    @Override
    public void adviceSince(AdviceSinceRequest request, StreamObserver<AdviceChanges> observer) {
        var delta = adviceService.adviceSince(
                UUID.fromString(request.getUserId()), ProtoTime.toInstant(request.getCursor()));
        AdviceChanges response = AdviceChanges.newBuilder()
                .addAllDeliveries(
                        delta.deliveries().stream().map(AdviceProtoMapper::toProto).toList())
                .setNextCursor(ProtoTime.toProto(delta.nextCursor()))
                .build();
        observer.onNext(response);
        observer.onCompleted();
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
