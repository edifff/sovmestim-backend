package ru.sovmestim.advice.client;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.proto.catalog.v1.CatalogServiceGrpc;
import ru.sovmestim.proto.catalog.v1.GetMedicineRequest;
import ru.sovmestim.proto.catalog.v1.GetSubstancesByIdsRequest;
import ru.sovmestim.proto.catalog.v1.GetSubstancesForMedicineRequest;
import ru.sovmestim.proto.catalog.v1.GetSubstancesForMedicinesRequest;
import ru.sovmestim.proto.catalog.v1.Medicine;
import ru.sovmestim.proto.catalog.v1.ResolveDrugRequest;
import ru.sovmestim.proto.catalog.v1.ResolveDrugResponse;

/**
 * gRPC catalog client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcCatalogClient implements CatalogClient {

    private static final long DEADLINE_SECONDS = 2;

    private final GrpcClientFactory channels;

    @Override
    public MedicineInfo medicine(UUID id) {
        Medicine medicine = stub().getMedicine(GetMedicineRequest.newBuilder().setId(id.toString()).build());
        return new MedicineInfo(
                UUID.fromString(medicine.getId()),
                medicine.getName(),
                medicine.getSubstancesList().stream().map(ContractMapper::toRef).toList());
    }

    @Override
    public Optional<ResolvedDrug> resolve(String name) {
        ResolveDrugResponse response =
                stub().resolveDrug(ResolveDrugRequest.newBuilder().setName(name).build());
        if (!response.getFound()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedDrug(
                ProtoIds.toUuid(response.getMedicineId()),
                response.getName(),
                response.getSubstancesList().stream().map(ContractMapper::toRef).toList()));
    }

    @Override
    public List<SubstanceRef> substancesByIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        GetSubstancesByIdsRequest request = GetSubstancesByIdsRequest.newBuilder()
                .addAllIds(ids.stream().map(UUID::toString).toList())
                .build();
        return stub().getSubstancesByIds(request).getSubstancesList().stream()
                .map(ContractMapper::toRef)
                .toList();
    }

    @Override
    public List<SubstanceRef> substancesForMedicine(UUID medicineId) {
        GetSubstancesForMedicineRequest request = GetSubstancesForMedicineRequest.newBuilder()
                .setMedicineId(medicineId.toString())
                .build();
        return stub().getSubstancesForMedicine(request).getSubstancesList().stream()
                .map(ContractMapper::toRef)
                .toList();
    }

    @Override
    public List<SubstanceRef> substancesForMedicines(Collection<UUID> medicineIds) {
        if (medicineIds.isEmpty()) {
            return List.of();
        }
        GetSubstancesForMedicinesRequest request = GetSubstancesForMedicinesRequest.newBuilder()
                .addAllMedicineIds(medicineIds.stream().map(UUID::toString).toList())
                .build();
        return stub().getSubstancesForMedicines(request).getSubstancesList().stream()
                .map(ContractMapper::toRef)
                .toList();
    }

    private CatalogServiceGrpc.CatalogServiceBlockingStub stub() {
        return CatalogServiceGrpc.newBlockingStub(channels.channel("catalog"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
