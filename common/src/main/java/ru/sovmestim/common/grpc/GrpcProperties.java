package ru.sovmestim.common.grpc;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * gRPC settings of one service: the port it serves on (absent for client-only processes) and the
 * endpoints of the services it calls.
 *
 * @param port the gRPC listen port, or {@code null} when the process does not serve gRPC
 * @param clients endpoint per called service, keyed by service name such as {@code catalog}
 */
@ConfigurationProperties(prefix = "sovmestim.grpc")
public record GrpcProperties(Integer port, Map<String, Endpoint> clients) {

    /**
     * Creates the settings, treating a missing client map as empty.
     *
     * @param port the gRPC listen port
     * @param clients endpoint per called service
     */
    public GrpcProperties {
        clients = clients != null ? Map.copyOf(clients) : Map.of();
    }

    /**
     * Address of one called service.
     *
     * @param host the host name
     * @param port the gRPC port
     */
    public record Endpoint(String host, int port) {
    }
}
