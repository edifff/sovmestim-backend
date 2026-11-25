package ru.sovmestim.common.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import ru.sovmestim.common.error.ApiException;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.ConflictException;
import ru.sovmestim.common.error.NotFoundException;

/**
 * Translates the domain exceptions of the services into stable gRPC status codes, so callers never
 * see a generic {@code UNKNOWN}. The gateway translates these codes back into the HTTP contract.
 */
public class GrpcExceptionInterceptor implements ServerInterceptor {

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(
            ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        return new FailureMappingListener<>(next.startCall(call, headers), call);
    }

    /**
     * Maps an application exception to a gRPC status.
     *
     * @param error the thrown exception
     * @return the status reported to the caller
     */
    static Status statusOf(RuntimeException error) {
        if (error instanceof BadRequestException) {
            return Status.INVALID_ARGUMENT;
        }
        if (error instanceof NotFoundException) {
            return Status.NOT_FOUND;
        }
        if (error instanceof ConflictException) {
            return Status.ABORTED;
        }
        if (error instanceof ApiException apiException) {
            return Status.FAILED_PRECONDITION.withDescription(apiException.getMessage());
        }
        return Status.INTERNAL;
    }

    /**
     * Closes the call as soon as the service implementation throws.
     *
     * @param <Q> request type of the call
     */
    private static final class FailureMappingListener<Q>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<Q> {

        private final ServerCall<?, ?> call;

        private FailureMappingListener(ServerCall.Listener<Q> delegate, ServerCall<?, ?> call) {
            super(delegate);
            this.call = call;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException error) {
                closeWith(error);
            }
        }

        @Override
        public void onMessage(Q message) {
            try {
                super.onMessage(message);
            } catch (RuntimeException error) {
                closeWith(error);
            }
        }

        private void closeWith(RuntimeException error) {
            Status status = statusOf(error);
            call.close(status.withDescription(error.getMessage()).withCause(error), new Metadata());
        }
    }
}
