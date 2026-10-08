package ru.sovmestim.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.config.JwtProperties;
import ru.sovmestim.config.OtpProperties;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.domain.OtpCode;
import ru.sovmestim.identity.domain.RefreshToken;
import ru.sovmestim.identity.dto.RequestCodeResponse;
import ru.sovmestim.identity.dto.TokenResponse;
import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.identity.repository.OtpCodeRepository;
import ru.sovmestim.identity.repository.RefreshTokenRepository;

/**
 * Passwordless login: request a code for an e-mail, verify it, then receive an access token and a
 * rotating refresh token.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final AppUserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;
    private final JwtService jwtService;
    private final OtpProperties otpProperties;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AppUserRepository userRepository,
            OtpCodeRepository otpCodeRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            OtpSender otpSender,
            JwtService jwtService,
            OtpProperties otpProperties,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpSender = otpSender;
        this.jwtService = jwtService;
        this.otpProperties = otpProperties;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RequestCodeResponse requestCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        String code = generateCode();
        Instant expiresAt = Instant.now().plus(otpProperties.ttl());
        OtpCode otp = OtpCode.builder()
                .email(email)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(expiresAt)
                .consumed(false)
                .attempts(0)
                .createdAt(Instant.now())
                .build();
        otpCodeRepository.save(otp);
        otpSender.send(email, code);
        String devCode = otpProperties.debugReturnCode() ? code : null;
        return new RequestCodeResponse(email, expiresAt, devCode);
    }

    @Transactional
    public TokenResponse verifyCode(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        OtpCode otp = otpCodeRepository.findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("No active login code for this e-mail"));
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            otp.setConsumed(true);
            otpCodeRepository.save(otp);
            throw new BadRequestException("Login code expired");
        }
        if (otp.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new BadRequestException("Too many attempts, request a new code");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpCodeRepository.save(otp);
            throw new BadRequestException("Invalid login code");
        }
        otp.setConsumed(true);
        otpCodeRepository.save(otp);

        AppUser user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                AppUser.builder().email(email).updatedAt(Instant.now()).build()));
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Unknown refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token is no longer valid");
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        AppUser user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BadRequestException("Unknown user for refresh token"));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(AppUser user) {
        String accessToken = jwtService.issueAccessToken(user);
        String rawRefresh = generateOpaqueToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(rawRefresh))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTtl()))
                .revoked(false)
                .createdAt(Instant.now())
                .build());
        log.debug("Issued tokens for user {}", user.getId());
        return TokenResponse.of(accessToken, rawRefresh, jwtService.accessTtlSeconds());
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    static String normalizeEmail(String email) {
        return Optional.ofNullable(email).orElse("").trim().toLowerCase(java.util.Locale.ROOT);
    }
}
