package ru.sovmestim.common.grpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Creates and caches one channel per called service. Channels are long-lived and reused by every
 * stub; they are shut down with the Spring context.
 */
public class GrpcClientFactory implements AutoCloseable {

    private static final String DEFAULT_HOST = "localhost";

    private final Map<String, GrpcProperties.Endpoint> endpoints;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    /**
     * Creates the factory from the configured endpoints.
     *
     * @param properties the gRPC properties of the service
     */
    public GrpcClientFactory(GrpcProperties properties) {
        this.endpoints = properties.clients();
    }

    /**
     * Returns the channel to the named service, creating it on first use.
     *
     * @param name the configured service name, such as {@code catalog}
     * @return the channel to that service
     */
    public ManagedChannel channel(String name) {
        return channels.computeIfAbsent(name, this::build);
    }

    private ManagedChannel build(String name) {
        GrpcProperties.Endpoint endpoint = endpoints.get(name);
        if (endpoint == null) {
            throw new IllegalStateException("No gRPC endpoint configured for service " + name);
        }
        String host = endpoint.host() != null && !endpoint.host().isBlank() ? endpoint.host() : DEFAULT_HOST;
        return ManagedChannelBuilder.forAddress(host, endpoint.port())
                .usePlaintext()
                .intercept(new CorrelationClientInterceptor())
                .build();
    }

    @Override
    public void close() {
        channels.values().forEach(ManagedChannel::shutdown);
        channels.clear();
    }
}
