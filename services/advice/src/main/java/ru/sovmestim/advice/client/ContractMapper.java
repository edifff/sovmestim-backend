package ru.sovmestim.advice.client;

import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.common.grpc.ProtoIds;
import ru.sovmestim.proto.catalog.v1.Substance;

/**
 * Shared conversion from contract messages to advice model objects.
 */
final class ContractMapper {

    private ContractMapper() {
    }

    static SubstanceRef toRef(Substance substance) {
        return new SubstanceRef(
                ProtoIds.toUuid(substance.getId()),
                substance.getName(),
                textOrNull(substance.getAtcCode()),
                textOrNull(substance.getAtcName()),
                textOrNull(substance.getPhg()));
    }

    static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
