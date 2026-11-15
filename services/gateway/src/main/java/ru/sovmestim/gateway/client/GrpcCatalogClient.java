package ru.sovmestim.gateway.client;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.gateway.dto.MedicineView;
import ru.sovmestim.gateway.dto.ResolvedDrug;
import ru.sovmestim.gateway.dto.SubstanceView;
import ru.sovmestim.proto.catalog.v1.CatalogServiceGrpc;
import ru.sovmestim.proto.catalog.v1.GetMedicineRequest;
import ru.sovmestim.proto.catalog.v1.ResolveDrugRequest;
import ru.sovmestim.proto.catalog.v1.SearchMedicinesRequest;
import ru.sovmestim.proto.catalog.v1.SearchSubstancesRequest;

/**
 * gRPC catalog client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcCatalogClient implements CatalogClient {

    private static final long DEADLINE_SECONDS = 3;

    private final GrpcClientFactory channels;

    @Override
    public List<SubstanceView> searchSubstances(String query, int limit) {
        return stub()
                .searchSubstances(SearchSubstancesRequest.newBuilder().setQuery(query).setLimit(limit).build())
                .getSubstancesList()
                .stream()
                .map(GatewayProtoMapper::substanceView)
                .toList();
    }

    @Override
    public List<MedicineView> searchMedicines(String query, int limit) {
        return stub()
                .searchMedicines(SearchMedicinesRequest.newBuilder().setQuery(query).setLimit(limit).build())
                .getMedicinesList()
                .stream()
                .map(GatewayProtoMapper::medicineView)
                .toList();
    }

    @Override
    public MedicineView medicine(UUID id) {
        return GatewayProtoMapper.medicineView(
                stub().getMedicine(GetMedicineRequest.newBuilder().setId(id.toString()).build()));
    }

    @Override
    public ResolvedDrug resolve(String name) {
        return GatewayProtoMapper.resolvedDrug(
                stub().resolveDrug(ResolveDrugRequest.newBuilder().setName(name).build()));
    }

    private CatalogServiceGrpc.CatalogServiceBlockingStub stub() {
        return CatalogServiceGrpc.newBlockingStub(channels.channel("catalog"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
