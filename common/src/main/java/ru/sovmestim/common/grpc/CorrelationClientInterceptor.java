package ru.sovmestim.common.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Forwards the current correlation id on every outgoing gRPC call, so one client request can be
 * traced through all services.
 */
public class CorrelationClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> CORRELATION_KEY =
            Metadata.Key.of(Correlation.HEADER, Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <Q, R> ClientCall<Q, R> interceptCall(
            MethodDescriptor<Q, R> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<R> responseListener, Metadata headers) {
                headers.put(CORRELATION_KEY, Correlation.currentOrCreate());
                super.start(responseListener, headers);
            }
        };
    }
}
