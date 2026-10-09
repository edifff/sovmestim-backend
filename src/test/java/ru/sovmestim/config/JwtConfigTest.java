package ru.sovmestim.config;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.service.JwtService;

/**
 * Tests that the JWT decoder only accepts tokens from the configured issuer.
 */
class JwtConfigTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef";
    private static final String ISSUER_A = "issuer-a";
    private static final String ISSUER_B = "issuer-b";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(5);
    private static final Duration REFRESH_TTL = Duration.ofDays(1);

    private final JwtConfig config = new JwtConfig();

    @Test
    void acceptsTokenSignedByConfiguredIssuer() {
        JwtProperties properties = properties(ISSUER_A);

        String token = service(properties).issueAccessToken(user());

        Assertions.assertThat(config.jwtDecoder(properties).decode(token).getClaimAsString("iss"))
                .isEqualTo(ISSUER_A);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtDecoder decoder = config.jwtDecoder(properties(ISSUER_A));
        String token = service(properties(ISSUER_B)).issueAccessToken(user());

        Assertions.assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    private JwtService service(JwtProperties properties) {
        JwtEncoder encoder = config.jwtEncoder(properties);
        return new JwtService(encoder, properties, Clock.systemUTC());
    }

    private static JwtProperties properties(String issuer) {
        return new JwtProperties(SECRET, issuer, ACCESS_TTL, REFRESH_TTL);
    }

    private static AppUser user() {
        return AppUser.builder().id(UUID.randomUUID()).email("user@example.com").build();
    }
}
