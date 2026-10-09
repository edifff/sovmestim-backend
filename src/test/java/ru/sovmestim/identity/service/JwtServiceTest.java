package ru.sovmestim.identity.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import ru.sovmestim.config.JwtProperties;
import ru.sovmestim.identity.domain.AppUser;

/**
 * Tests for the HS256 access-token issuer.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef";
    private static final String ISSUER = "sovmestim-test";
    private static final String EMAIL = "user@example.com";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(5);

    @Test
    void issuesDecodableTokenWithUserIdSubjectAndEmailClaim() {
        JwtService service = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key())),
                new JwtProperties(SECRET, ISSUER, ACCESS_TTL, Duration.ofDays(1)), Clock.systemUTC());
        UUID userId = UUID.randomUUID();
        AppUser user = AppUser.builder().id(userId).email(EMAIL).updatedAt(Instant.now()).build();

        Jwt decoded = NimbusJwtDecoder.withSecretKey(key()).build().decode(service.issueAccessToken(user));

        Assertions.assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        Assertions.assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
        Assertions.assertThat(decoded.getClaimAsString("email")).isEqualTo(EMAIL);
        Assertions.assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
        Assertions.assertThat(service.accessTtlSeconds()).isEqualTo(ACCESS_TTL.toSeconds());
    }

    private static SecretKey key() {
        return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
