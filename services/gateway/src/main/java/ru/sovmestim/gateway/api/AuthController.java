package ru.sovmestim.gateway.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import ru.sovmestim.gateway.client.IdentityClient;
import ru.sovmestim.gateway.client.TokenResult;
import ru.sovmestim.gateway.dto.RefreshTokenRequest;
import ru.sovmestim.gateway.dto.RequestCodeRequest;
import ru.sovmestim.gateway.dto.RequestCodeResponse;
import ru.sovmestim.gateway.dto.TokenResponse;
import ru.sovmestim.gateway.dto.VerifyCodeRequest;

/**
 * Passwordless authentication endpoints. The work is done by the identity service; this controller
 * only maps the REST contract onto gRPC.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
public class AuthController {

    private final IdentityClient identityClient;

    /**
     * Starts a login by sending a one-time code to the e-mail.
     *
     * @param request validated request carrying the e-mail address
     * @return the code expiry and, in dev mode, the code itself
     */
    @PostMapping("/request-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestCodeResponse requestCode(@Valid @RequestBody RequestCodeRequest request) {
        var result = identityClient.requestCode(request.email());
        return new RequestCodeResponse(result.email(), result.expiresAt(), result.devCode());
    }

    /**
     * Completes a login by verifying the submitted one-time code.
     *
     * @param request validated request carrying the e-mail address and the code
     * @return the issued access and refresh tokens
     */
    @PostMapping("/verify")
    public TokenResponse verify(@Valid @RequestBody VerifyCodeRequest request) {
        return token(identityClient.verifyCode(request.email(), request.code()));
    }

    /**
     * Rotates a refresh token into a new token pair.
     *
     * @param request validated request carrying the refresh token
     * @return the issued access and refresh tokens
     */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return token(identityClient.refresh(request.refreshToken()));
    }

    private static TokenResponse token(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(), result.tokenType(), result.expiresIn());
    }
}
