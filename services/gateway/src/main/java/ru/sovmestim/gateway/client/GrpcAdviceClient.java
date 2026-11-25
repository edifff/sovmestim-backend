package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.gateway.dto.AdviceCheckRequest;
import ru.sovmestim.gateway.dto.AdviceCheckResponse;
import ru.sovmestim.proto.advice.v1.AdviceServiceGrpc;
import ru.sovmestim.proto.advice.v1.AdviceSinceRequest;
import ru.sovmestim.proto.advice.v1.CheckRequest;
import ru.sovmestim.proto.advice.v1.RecheckAllRequest;
import ru.sovmestim.proto.advice.v1.RecheckCoursesRequest;

/**
 * gRPC advice client. The check deadline is longer because a live RLS source may be consulted.
 */
@Component
@RequiredArgsConstructor
public class GrpcAdviceClient implements AdviceClient {

    private static final long DEADLINE_SECONDS = 15;

    private final GrpcClientFactory channels;

    @Override
    public AdviceCheckResponse check(UUID userId, AdviceCheckRequest request) {
        CheckRequest.Builder builder = CheckRequest.newBuilder()
                .setUserId(userId.toString())
                .setDrugName(GatewayProtoMapper.text(request.drugName()))
                .setMedicineId(ProtoIds.toText(request.medicineId()));
        if (request.substanceIds() != null) {
            builder.addAllSubstanceIds(request.substanceIds().stream().map(UUID::toString).toList());
        }
        return GatewayProtoMapper.adviceCheck(stub().check(builder.build()));
    }

    @Override
    public List<AdviceCheckResponse> recheckCourses(UUID userId, Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        RecheckCoursesRequest request = RecheckCoursesRequest.newBuilder()
                .setUserId(userId.toString())
                .addAllCourseIds(courseIds.stream().map(UUID::toString).toList())
                .build();
        return stub().recheckCourses(request).getResponsesList().stream()
                .map(GatewayProtoMapper::adviceCheck)
                .toList();
    }

    @Override
    public List<AdviceCheckResponse> recheckAll(UUID userId) {
        return stub()
                .recheckAll(RecheckAllRequest.newBuilder().setUserId(userId.toString()).build())
                .getResponsesList()
                .stream()
                .map(GatewayProtoMapper::adviceCheck)
                .toList();
    }

    @Override
    public AdviceDelta adviceSince(UUID userId, Instant cursor) {
        AdviceSinceRequest.Builder builder = AdviceSinceRequest.newBuilder().setUserId(userId.toString());
        if (cursor != null) {
            builder.setCursor(ProtoTime.toProto(cursor));
        }
        var response = stub().adviceSince(builder.build());
        return new AdviceDelta(
                response.getDeliveriesList().stream().map(GatewayProtoMapper::adviceDelivery).toList(),
                ProtoTime.toInstant(response.getNextCursor()));
    }

    private AdviceServiceGrpc.AdviceServiceBlockingStub stub() {
        return AdviceServiceGrpc.newBlockingStub(channels.channel("advice"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
