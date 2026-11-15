package ru.sovmestim.identity.grpc;

import ru.sovmestim.common.grpc.ProtoTime;
import ru.sovmestim.identity.dto.RequestCodeResponse;
import ru.sovmestim.identity.dto.TokenResponse;
import ru.sovmestim.identity.dto.UserResponse;
import ru.sovmestim.proto.identity.v1.User;

/**
 * Maps the identity use-case results to the generated contract types.
 */
final class IdentityProtoMapper {

    private IdentityProtoMapper() {
    }

    static ru.sovmestim.proto.identity.v1.RequestCodeResponse toProto(RequestCodeResponse response) {
        return ru.sovmestim.proto.identity.v1.RequestCodeResponse.newBuilder()
                .setEmail(response.email())
                .setExpiresAt(ProtoTime.toProto(response.expiresAt()))
                .setDevCode(response.devCode() != null ? response.devCode() : "")
                .build();
    }

    static ru.sovmestim.proto.identity.v1.TokenResponse toProto(TokenResponse response) {
        return ru.sovmestim.proto.identity.v1.TokenResponse.newBuilder()
                .setAccessToken(response.accessToken())
                .setRefreshToken(response.refreshToken())
                .setTokenType(response.tokenType())
                .setExpiresIn(response.expiresIn())
                .build();
    }

    static User toProto(UserResponse response) {
        User.Builder user = User.newBuilder()
                .setId(response.id())
                .setEmail(text(response.email()))
                .setSurname(text(response.surname()))
                .setName(text(response.name()));
        if (response.birthDate() != null) {
            user.setBirthDate(response.birthDate().toString());
        }
        return user.build();
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
