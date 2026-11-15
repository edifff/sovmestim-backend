package ru.sovmestim.identity.grpc;

import java.util.UUID;

import io.grpc.stub.StreamObserver;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.identity.service.AuthService;
import ru.sovmestim.identity.service.UserService;
import ru.sovmestim.proto.identity.v1.GetUserRequest;
import ru.sovmestim.proto.identity.v1.IdentityServiceGrpc;
import ru.sovmestim.proto.identity.v1.RefreshTokenRequest;
import ru.sovmestim.proto.identity.v1.RequestCodeRequest;
import ru.sovmestim.proto.identity.v1.TokenResponse;
import ru.sovmestim.proto.identity.v1.User;
import ru.sovmestim.proto.identity.v1.VerifyCodeRequest;

/**
 * gRPC surface of the identity service. Exceptions propagate to the common interceptor, which maps
 * them to stable status codes.
 */
@Component
@RequiredArgsConstructor
public class IdentityGrpcEndpoint extends IdentityServiceGrpc.IdentityServiceImplBase {

    private final AuthService authService;
    private final UserService userService;

    @Override
    public void requestCode(RequestCodeRequest request, StreamObserver<ru.sovmestim.proto.identity.v1.RequestCodeResponse> observer) {
        observer.onNext(IdentityProtoMapper.toProto(authService.requestCode(request.getEmail())));
        observer.onCompleted();
    }

    @Override
    public void verifyCode(VerifyCodeRequest request, StreamObserver<TokenResponse> observer) {
        observer.onNext(IdentityProtoMapper.toProto(authService.verifyCode(request.getEmail(), request.getCode())));
        observer.onCompleted();
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<TokenResponse> observer) {
        observer.onNext(IdentityProtoMapper.toProto(authService.refresh(request.getRefreshToken())));
        observer.onCompleted();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<User> observer) {
        observer.onNext(IdentityProtoMapper.toProto(userService.get(UUID.fromString(request.getUserId()))));
        observer.onCompleted();
    }
}
