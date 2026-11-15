package ru.sovmestim.common.grpc;

import java.util.List;

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires gRPC into a service: starts a server with every {@link BindableService} bean when
 * {@code sovmestim.grpc.port} is set, and always provides the client channel factory.
 */
@Configuration
@EnableConfigurationProperties(GrpcProperties.class)
public class GrpcConfiguration {

    /**
     * Builds the server lifecycle for all gRPC endpoints of the service.
     *
     * @param services every gRPC endpoint registered in the context
     * @param properties the gRPC settings
     * @param correlation the correlation-id server interceptor
     * @param errors the domain-exception server interceptor
     * @return the lifecycle that starts and stops the server
     */
    @Bean
    @ConditionalOnProperty(prefix = "sovmestim.grpc", name = "port")
    public GrpcServerLifecycle grpcServerLifecycle(
            List<BindableService> services,
            GrpcProperties properties,
            CorrelationServerInterceptor correlation,
            GrpcExceptionInterceptor errors) {
        ServerBuilder<?> builder =
                Grpc.newServerBuilderForPort(properties.port(), InsecureServerCredentials.create());
        for (BindableService service : services) {
            builder.addService(ServerInterceptors.intercept(service, correlation, errors));
        }
        Server server = builder.build();
        return new GrpcServerLifecycle(server, properties.port());
    }

    /**
     * Creates the client channel factory.
     *
     * @param properties the gRPC settings
     * @return the factory used by every outbound client
     */
    @Bean
    public GrpcClientFactory grpcClientFactory(GrpcProperties properties) {
        return new GrpcClientFactory(properties);
    }

    /**
     * Creates the correlation-id server interceptor.
     *
     * @return the interceptor
     */
    @Bean
    public CorrelationServerInterceptor correlationServerInterceptor() {
        return new CorrelationServerInterceptor();
    }

    /**
     * Creates the domain-exception server interceptor.
     *
     * @return the interceptor
     */
    @Bean
    public GrpcExceptionInterceptor grpcExceptionInterceptor() {
        return new GrpcExceptionInterceptor();
    }
}
