package ru.sovmestim.common.grpc;

import java.time.Instant;

import com.google.protobuf.Timestamp;

/**
 * Converts between {@link Instant} and the protobuf timestamp. A missing or epoch-zero timestamp is
 * treated as {@code null}, which is how the optional date fields of the contracts are modelled.
 */
public final class ProtoTime {

    private ProtoTime() {
    }

    /**
     * Converts an instant to a protobuf timestamp.
     *
     * @param instant the instant, may be {@code null}
     * @return the timestamp, or the default instance when the instant is {@code null}
     */
    public static Timestamp toProto(Instant instant) {
        if (instant == null) {
            return Timestamp.getDefaultInstance();
        }
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    /**
     * Converts a protobuf timestamp to an instant.
     *
     * @param timestamp the timestamp, may be {@code null}
     * @return the instant, or {@code null} for a missing or epoch-zero timestamp
     */
    public static Instant toInstant(Timestamp timestamp) {
        if (timestamp == null || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
