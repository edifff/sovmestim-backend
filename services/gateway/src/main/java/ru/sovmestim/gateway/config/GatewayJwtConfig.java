package ru.sovmestim.gateway.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Wires the HS256 access-token decoder. The gateway never issues tokens: identity owns the encoder.
 */
@Configuration
public class GatewayJwtConfig {

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey(properties)).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    private static SecretKey secretKey(JwtProperties properties) {
        byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "sovmestim.jwt.secret must be at least 32 bytes for HS256 (got " + bytes.length + ")");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
