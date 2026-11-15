package ru.sovmestim.common.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * Reads the correlation id from the incoming metadata, publishes it to the MDC for the duration of
 * the call and generates one when the caller did not send it.
 */
public class CorrelationServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> CORRELATION_KEY =
            Metadata.Key.of(Correlation.HEADER, Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(
            ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        String correlationId = headers.get(CORRELATION_KEY);
        Correlation.put(correlationId != null && !correlationId.isBlank()
                ? correlationId
                : Correlation.currentOrCreate());
        return new ClearOnCompletionListener<>(next.startCall(call, headers));
    }

    /**
     * Clears the MDC entry when the call completes or is cancelled.
     *
     * @param <Q> request type of the call
     */
    private static final class ClearOnCompletionListener<Q>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<Q> {

        private ClearOnCompletionListener(ServerCall.Listener<Q> delegate) {
            super(delegate);
        }

        @Override
        public void onComplete() {
            try {
                super.onComplete();
            } finally {
                Correlation.clear();
            }
        }

        @Override
        public void onCancel() {
            try {
                super.onCancel();
            } finally {
                Correlation.clear();
            }
        }
    }
}
