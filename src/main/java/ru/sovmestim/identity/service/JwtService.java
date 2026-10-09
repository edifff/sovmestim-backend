package ru.sovmestim.identity.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.config.JwtProperties;
import ru.sovmestim.identity.domain.AppUser;

/**
 * Issues short-lived HS256 access tokens. The subject is the user id; the client sends it as
 * {@code Authorization: Bearer ...}.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    /**
     * Issues a short-lived HS256 access token for the user.
     *
     * @param user the user the token is issued for.
     * @return the signed JWT as a string.
     */
    public String issueAccessToken(AppUser user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTtl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Returns the configured access token lifetime.
     *
     * @return the access token time-to-live in seconds.
     */
    public long accessTtlSeconds() {
        return properties.accessTtl().toSeconds();
    }
}
