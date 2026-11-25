package ru.sovmestim.intake.client;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.grpc.StatusRuntimeException;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.proto.catalog.v1.CatalogServiceGrpc;
import ru.sovmestim.proto.catalog.v1.GetMedicineRequest;
import ru.sovmestim.proto.catalog.v1.Medicine;
import ru.sovmestim.proto.catalog.v1.ResolveDrugRequest;
import ru.sovmestim.proto.catalog.v1.ResolveDrugResponse;

/**
 * gRPC implementation of {@link CatalogClient} with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcCatalogClient implements CatalogClient {

    private static final long DEADLINE_SECONDS = 2;

    private final GrpcClientFactory channels;

    @Override
    public ResolvedMedicine medicine(UUID id) {
        try {
            Medicine medicine = stub().getMedicine(GetMedicineRequest.newBuilder().setId(id.toString()).build());
            return new ResolvedMedicine(UUID.fromString(medicine.getId()), medicine.getName(), medicine.getBrand());
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                throw new NotFoundException("Medicine not found: " + id);
            }
            throw ex;
        }
    }

    @Override
    public Optional<ResolvedMedicine> resolve(String name) {
        ResolveDrugResponse response =
                stub().resolveDrug(ResolveDrugRequest.newBuilder().setName(name).build());
        if (!response.getFound() || response.getMedicineId().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(medicine(UUID.fromString(response.getMedicineId())));
    }

    private CatalogServiceGrpc.CatalogServiceBlockingStub stub() {
        return CatalogServiceGrpc.newBlockingStub(channels.channel("catalog"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
