package ru.sovmestim.common.grpc;

import io.grpc.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Starts and stops the gRPC server together with the Spring context, so a service exposes both its
 * actuator HTTP port and its gRPC port for the whole application lifetime.
 */
public class GrpcServerLifecycle implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private static final int SHUTDOWN_SECONDS = 5;

    private final Server server;
    private final int port;
    private volatile boolean running;

    /**
     * Creates the lifecycle for a built server.
     *
     * @param server the gRPC server to manage
     * @param port the port the server listens on, used for logging
     */
    public GrpcServerLifecycle(Server server, int port) {
        this.server = server;
        this.port = port;
    }

    @Override
    public void start() {
        try {
            server.start();
            running = true;
            LOG.info("gRPC server started on port {}", port);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Cannot start the gRPC server on port " + port, ex);
        }
    }

    @Override
    public void stop() {
        running = false;
        server.shutdown();
        try {
            if (!server.awaitTermination(SHUTDOWN_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            server.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
