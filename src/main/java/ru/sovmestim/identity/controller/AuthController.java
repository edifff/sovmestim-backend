package ru.sovmestim.identity.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/request-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestCodeResponse requestCode(@Valid @RequestBody RequestCodeRequest request) {
        return authService.requestCode(request.email());
    }

    @PostMapping("/verify")
    public TokenResponse verify(@Valid @RequestBody VerifyCodeRequest request) {
        return authService.verifyCode(request.email(), request.code());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
