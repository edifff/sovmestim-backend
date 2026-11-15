package ru.sovmestim.identity;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.identity.service.AuthService;
import ru.sovmestim.identity.service.UserService;

/**
 * Identity service against a real PostgreSQL: passwordless login end to end and profile lookup.
 */
@SpringBootTest(properties = {
        "sovmestim.grpc.port=0",
        "sovmestim.otp.debug-return-code=true",
        "sovmestim.otp.channel=log"
})
class IdentityIT {

    private static final String EMAIL_PREFIX = "user-";
    private static final String EMAIL_SUFFIX = "@example.com";

    private static final String DB_USER = "sovmestim";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sovmestim_identity")
            .withUsername(DB_USER)
            .withPassword(DB_USER);

    static {
        POSTGRES.start();
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository userRepository;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void codeLoginCreatesTheUserAndIssuesTokens() {
        String email = EMAIL_PREFIX + UUID.randomUUID() + EMAIL_SUFFIX;

        var requested = authService.requestCode(email);
        var tokens = authService.verifyCode(email, requested.devCode());

        Assertions.assertThat(tokens.accessToken()).isNotBlank();
        Assertions.assertThat(tokens.refreshToken()).isNotBlank();
        Assertions.assertThat(authService.refresh(tokens.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void profileIsAvailableAfterLogin() {
        String email = EMAIL_PREFIX + UUID.randomUUID() + EMAIL_SUFFIX;
        var requested = authService.requestCode(email);
        authService.verifyCode(email, requested.devCode());

        var stored = userRepository.findByEmail(email).orElseThrow();
        var profile = userService.get(stored.getId());

        Assertions.assertThat(profile.email()).isEqualTo(email);
    }
}
