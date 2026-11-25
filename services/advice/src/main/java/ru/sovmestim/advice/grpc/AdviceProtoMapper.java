package ru.sovmestim.advice.grpc;

import ru.sovmestim.advice.model.AdviceFinding;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.model.AdviceSourceRef;
import ru.sovmestim.advice.service.AdviceDeliveryView;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.proto.advice.v1.AdviceDelivery;
import ru.sovmestim.proto.advice.v1.AdviceResponse;
import ru.sovmestim.proto.advice.v1.AdviceSource;

/**
 * Maps advice engine results to the generated contract types.
 */
final class AdviceProtoMapper {

    private AdviceProtoMapper() {
    }

    static AdviceResponse toProto(ru.sovmestim.advice.dto.AdviceCheckResponse response) {
        return AdviceResponse.newBuilder()
                .setAdviceId(response.adviceId().toString())
                .setResult(toProto(response.result()))
                .build();
    }

    static ru.sovmestim.proto.advice.v1.AdviceResult toProto(AdviceResult result) {
        return ru.sovmestim.proto.advice.v1.AdviceResult.newBuilder()
                .setStatus(result.status() != null ? result.status().name() : "")
                .setLevel(result.level() != null ? result.level().name() : "")
                .addAllFindings(result.findings().stream().map(AdviceProtoMapper::toProto).toList())
                .setCheckedSubstances(result.checkedSubstances())
                .setRulesVersion(text(result.rulesVersion()))
                .setMappingVersion(text(result.mappingVersion()))
                .setCatalogVersion(text(result.catalogVersion()))
                .setCheckedAt(ProtoTime.toProto(result.checkedAt()))
                .addAllNotes(result.notes() != null ? result.notes() : java.util.List.of())
                .build();
    }

    static AdviceDelivery toProto(AdviceDeliveryView view) {
        return AdviceDelivery.newBuilder()
                .setAdviceId(view.adviceId())
                .setDrugName(text(view.drugName()))
                .setCreatedAt(ProtoTime.toProto(view.createdAt()))
                .setResult(toProto(view.result()))
                .build();
    }

    private static ru.sovmestim.proto.advice.v1.AdviceFinding toProto(AdviceFinding finding) {
        return ru.sovmestim.proto.advice.v1.AdviceFinding.newBuilder()
                .setKind(finding.kind() != null ? finding.kind().name() : "")
                .setLevel(finding.level() != null ? finding.level().name() : "")
                .setTitle(text(finding.title()))
                .setText(text(finding.text()))
                .addAllSubstances(finding.substances() != null ? finding.substances() : java.util.List.of())
                .addAllSources(finding.sources() != null
                        ? finding.sources().stream().map(AdviceProtoMapper::toProto).toList()
                        : java.util.List.of())
                .build();
    }

    private static AdviceSource toProto(AdviceSourceRef source) {
        return AdviceSource.newBuilder()
                .setKind(text(source.kind()))
                .setName(text(source.name()))
                .setDate(source.date() != null ? source.date().toString() : "")
                .setNote(text(source.note()))
                .build();
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
