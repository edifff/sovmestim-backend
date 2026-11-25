package ru.sovmestim.common.grpc;

import java.util.UUID;

/**
 * Converts identifiers to and from the string representation used by the contracts.
 */
public final class ProtoIds {

    private ProtoIds() {
    }

    /**
     * Parses a contract identifier.
     *
     * @param value the textual identifier, may be blank
     * @return the UUID, or {@code null} when the value is blank
     */
    public static UUID toUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    /**
     * Formats an identifier for the contract.
     *
     * @param value the UUID, may be {@code null}
     * @return the textual identifier, or an empty string when the value is {@code null}
     */
    public static String toText(UUID value) {
        return value != null ? value.toString() : "";
    }
}
