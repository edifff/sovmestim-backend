package ru.sovmestim.common.grpc;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * Correlation id propagation shared by HTTP and gRPC boundaries. The gateway creates the id, every
 * internal call forwards it in metadata, and every log line can include it from the MDC.
 */
public final class Correlation {

    /** Metadata / HTTP header carrying the correlation id. */
    public static final String HEADER = "x-correlation-id";

    /** MDC key under which the correlation id is stored. */
    public static final String MDC_KEY = "correlationId";

    private Correlation() {
    }

    /**
     * Returns the correlation id of the current request, creating one when absent.
     *
     * @return the current or newly created correlation id
     */
    public static String currentOrCreate() {
        String current = MDC.get(MDC_KEY);
        return current != null && !current.isBlank() ? current : UUID.randomUUID().toString();
    }

    /**
     * Puts a correlation id into the MDC.
     *
     * @param correlationId the id to store
     */
    public static void put(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    /**
     * Removes the correlation id from the MDC.
     */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
