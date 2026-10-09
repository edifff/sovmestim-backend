package ru.sovmestim.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import ru.sovmestim.identity.dto.RefreshTokenRequest;
import ru.sovmestim.identity.dto.RequestCodeRequest;
import ru.sovmestim.identity.dto.RequestCodeResponse;
import ru.sovmestim.identity.dto.TokenResponse;
import ru.sovmestim.identity.dto.VerifyCodeRequest;
import ru.sovmestim.identity.service.AuthService;

/**
 * Passwordless authentication endpoints (e-mail code / Telegram bot, SMS later behind OtpSender).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Starts a login by sending a one-time code to the e-mail.
     *
     * @param request validated request carrying the e-mail address.
     * @return the code expiry and, in dev mode, the code itself.
     */
    @PostMapping("/request-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestCodeResponse requestCode(@Valid @RequestBody RequestCodeRequest request) {
        return authService.requestCode(request.email());
    }

    /**
     * Completes a login by verifying the submitted one-time code.
     *
     * @param request validated request carrying the e-mail address and the code.
     * @return the issued access and refresh tokens.
     */
    @PostMapping("/verify")
    public TokenResponse verify(@Valid @RequestBody VerifyCodeRequest request) {
        return authService.verifyCode(request.email(), request.code());
    }

    /**
     * Rotates a refresh token into a new token pair.
     *
     * @param request validated request carrying the refresh token.
     * @return the issued access and refresh tokens.
     */
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
