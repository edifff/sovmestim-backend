package ru.sovmestim.catalog.grpc;

import java.util.List;
import java.util.UUID;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.catalog.dto.MedicineView;
import ru.sovmestim.catalog.dto.SubstanceDoseView;
import ru.sovmestim.catalog.dto.SubstanceRef;
import ru.sovmestim.catalog.dto.SubstanceView;
import ru.sovmestim.catalog.service.CatalogService;
import ru.sovmestim.catalog.service.ResolvedDrug;
import ru.sovmestim.proto.catalog.v1.CatalogServiceGrpc;
import ru.sovmestim.proto.catalog.v1.GetMedicineRequest;
import ru.sovmestim.proto.catalog.v1.GetSubstancesByIdsRequest;
import ru.sovmestim.proto.catalog.v1.GetSubstancesForMedicineRequest;
import ru.sovmestim.proto.catalog.v1.GetSubstancesForMedicinesRequest;
import ru.sovmestim.proto.catalog.v1.Medicine;
import ru.sovmestim.proto.catalog.v1.MedicineList;
import ru.sovmestim.proto.catalog.v1.ResolveDrugRequest;
import ru.sovmestim.proto.catalog.v1.ResolveDrugResponse;
import ru.sovmestim.proto.catalog.v1.SearchMedicinesRequest;
import ru.sovmestim.proto.catalog.v1.SearchSubstancesRequest;
import ru.sovmestim.proto.catalog.v1.Substance;
import ru.sovmestim.proto.catalog.v1.SubstanceList;

/**
 * gRPC surface of the catalog. Exceptions are deliberately not caught: the common exception
 * interceptor maps them to stable status codes for every service.
 */
@Component
@RequiredArgsConstructor
public class CatalogGrpcEndpoint extends CatalogServiceGrpc.CatalogServiceImplBase {

    private final CatalogService catalogService;

    @Override
    public void searchSubstances(SearchSubstancesRequest request, StreamObserver<SubstanceList> observer) {
        List<Substance> substances = catalogService
                .searchSubstances(request.getQuery(), request.getLimit())
                .stream()
                .map(CatalogGrpcEndpoint::toProto)
                .toList();
        observer.onNext(SubstanceList.newBuilder().addAllSubstances(substances).build());
        observer.onCompleted();
    }

    @Override
    public void searchMedicines(SearchMedicinesRequest request, StreamObserver<MedicineList> observer) {
        List<Medicine> medicines = catalogService
                .searchMedicines(request.getQuery(), request.getLimit())
                .stream()
                .map(CatalogGrpcEndpoint::toProto)
                .toList();
        observer.onNext(MedicineList.newBuilder().addAllMedicines(medicines).build());
        observer.onCompleted();
    }

    @Override
    public void getMedicine(GetMedicineRequest request, StreamObserver<Medicine> observer) {
        MedicineView medicine = catalogService.getMedicine(UUID.fromString(request.getId()));
        observer.onNext(toProto(medicine));
        observer.onCompleted();
    }

    @Override
    public void resolveDrug(ResolveDrugRequest request, StreamObserver<ResolveDrugResponse> observer) {
        ResolveDrugResponse.Builder response = ResolveDrugResponse.newBuilder().setName(request.getName());
        catalogService.resolveByName(request.getName()).ifPresent(drug -> fill(response, drug));
        observer.onNext(response.build());
        observer.onCompleted();
    }

    @Override
    public void getSubstancesByIds(GetSubstancesByIdsRequest request, StreamObserver<SubstanceList> observer) {
        List<UUID> ids = request.getIdsList().stream().map(UUID::fromString).toList();
        List<Substance> substances =
                catalogService.substancesByIds(ids).stream().map(CatalogGrpcEndpoint::toProto).toList();
        observer.onNext(SubstanceList.newBuilder().addAllSubstances(substances).build());
        observer.onCompleted();
    }

    @Override
    public void getSubstancesForMedicine(
            GetSubstancesForMedicineRequest request, StreamObserver<SubstanceList> observer) {
        List<Substance> substances = catalogService
                .substancesForMedicine(UUID.fromString(request.getMedicineId()))
                .stream()
                .map(CatalogGrpcEndpoint::toProto)
                .toList();
        observer.onNext(SubstanceList.newBuilder().addAllSubstances(substances).build());
        observer.onCompleted();
    }

    @Override
    public void getSubstancesForMedicines(
            GetSubstancesForMedicinesRequest request, StreamObserver<SubstanceList> observer) {
        List<UUID> ids = request.getMedicineIdsList().stream().map(UUID::fromString).toList();
        List<Substance> substances =
                catalogService.substancesForMedicines(ids).stream().map(CatalogGrpcEndpoint::toProto).toList();
        observer.onNext(SubstanceList.newBuilder().addAllSubstances(substances).build());
        observer.onCompleted();
    }

    private static void fill(ResolveDrugResponse.Builder response, ResolvedDrug drug) {
        response.setFound(true).setName(drug.name());
        if (drug.medicineId() != null) {
            response.setMedicineId(drug.medicineId().toString());
        }
        response.addAllSubstances(drug.substances().stream().map(CatalogGrpcEndpoint::toProto).toList());
    }

    private static Substance toProto(SubstanceView view) {
        return Substance.newBuilder()
                .setId(view.id().toString())
                .setName(view.name())
                .setAtcCode(text(view.atcCode()))
                .setAtcName(text(view.atcName()))
                .setDescription(text(view.description()))
                .build();
    }

    private static Substance toProto(SubstanceRef ref) {
        return Substance.newBuilder()
                .setId(ref.id() != null ? ref.id().toString() : "")
                .setName(text(ref.name()))
                .setAtcCode(text(ref.atcCode()))
                .setAtcName(text(ref.atcName()))
                .setPhg(text(ref.phg()))
                .build();
    }

    private static Substance toProto(SubstanceDoseView dose) {
        return Substance.newBuilder()
                .setId(dose.id().toString())
                .setName(dose.name())
                .setDosage(text(dose.dosage()))
                .build();
    }

    private static Medicine toProto(MedicineView view) {
        return Medicine.newBuilder()
                .setId(view.id().toString())
                .setName(view.name())
                .setBrand(text(view.brand()))
                .setFormRelease(text(view.formRelease()))
                .addAllSubstances(view.substances().stream().map(CatalogGrpcEndpoint::toProto).toList())
                .build();
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
