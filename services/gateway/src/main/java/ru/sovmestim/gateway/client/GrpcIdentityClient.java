package ru.sovmestim.gateway.client;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.proto.identity.v1.GetUserRequest;
import ru.sovmestim.proto.identity.v1.IdentityServiceGrpc;
import ru.sovmestim.proto.identity.v1.RefreshTokenRequest;
import ru.sovmestim.proto.identity.v1.RequestCodeRequest;
import ru.sovmestim.proto.identity.v1.TokenResponse;
import ru.sovmestim.proto.identity.v1.User;
import ru.sovmestim.proto.identity.v1.VerifyCodeRequest;

/**
 * gRPC identity client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcIdentityClient implements IdentityClient {

    private static final long DEADLINE_SECONDS = 5;

    private final GrpcClientFactory channels;

    @Override
    public RequestCodeResult requestCode(String email) {
        var response = stub().requestCode(RequestCodeRequest.newBuilder().setEmail(email).build());
        return new RequestCodeResult(
                response.getEmail(),
                ProtoTime.toInstant(response.getExpiresAt()),
                GatewayProtoMapper.textOrNull(response.getDevCode()));
    }

    @Override
    public TokenResult verifyCode(String email, String code) {
        return token(stub().verifyCode(
                VerifyCodeRequest.newBuilder().setEmail(email).setCode(code).build()));
    }

    @Override
    public TokenResult refresh(String refreshToken) {
        return token(stub().refreshToken(
                RefreshTokenRequest.newBuilder().setRefreshToken(refreshToken).build()));
    }

    @Override
    public UserInfo user(UUID userId) {
        User user = stub().getUser(GetUserRequest.newBuilder().setUserId(userId.toString()).build());
        return new UserInfo(
                user.getId(),
                GatewayProtoMapper.textOrNull(user.getEmail()),
                GatewayProtoMapper.textOrNull(user.getSurname()),
                GatewayProtoMapper.textOrNull(user.getName()),
                GatewayProtoMapper.date(user.getBirthDate()));
    }

    private static TokenResult token(TokenResponse response) {
        return new TokenResult(
                response.getAccessToken(),
                response.getRefreshToken(),
                response.getTokenType(),
                response.getExpiresIn());
    }

    private IdentityServiceGrpc.IdentityServiceBlockingStub stub() {
        return IdentityServiceGrpc.newBlockingStub(channels.channel("identity"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
