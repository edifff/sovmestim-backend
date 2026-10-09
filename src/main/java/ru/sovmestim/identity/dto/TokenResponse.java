package ru.sovmestim.identity.dto;

/**
 * Token pair returned by the login and refresh flows.
 *
 * @param accessToken short-lived JWT sent by the client as a bearer token.
 * @param refreshToken opaque token used to obtain a new token pair.
 * @param tokenType authorization scheme of the access token.
 * @param expiresIn access token lifetime in seconds.
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    /**
     * Creates a token response with the {@code Bearer} token type.
     *
     * @param accessToken short-lived JWT sent by the client as a bearer token.
     * @param refreshToken opaque token used to obtain a new token pair.
     * @param expiresIn access token lifetime in seconds.
     * @return the assembled token response.
     */
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
