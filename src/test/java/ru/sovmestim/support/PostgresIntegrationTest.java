package ru.sovmestim.support;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL via Testcontainers. AGENTS.md requires database behavior to be tested against a
 * real database rather than mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTest {

    private static final String DB_NAME = "sovmestim";
    private static final String ENABLED_PROPERTY_VALUE = "true";
    private static final String JSON_EMAIL_PREFIX = "{\"email\":\"";
    private static final String JSON_BODY_SUFFIX = "\"}";

    // Static so the container starts once and is shared across all context-cached test classes.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName(DB_NAME)
            .withUsername(DB_NAME)
            .withPassword(DB_NAME);

    static {
        POSTGRES.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("sovmestim.demo.seed", () -> ENABLED_PROPERTY_VALUE);
        registry.add("sovmestim.otp.debug-return-code", () -> ENABLED_PROPERTY_VALUE);
    }

    /**
     * Returns the MockMvc instance used to perform requests against the application.
     *
     * @return the shared MockMvc instance
     */
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    /**
     * Performs the full passwordless login and returns a bearer token.
     *
     * @return the bearer token issued by the login flow
     * @throws Exception if the login flow fails
     */
    protected String loginAndGetToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult requested = mockMvc
                .perform(MockMvcRequestBuilders.post("/v1/auth/request-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();
        String code = JsonPath.read(requested.getResponse().getContentAsString(), "$.devCode");

        MvcResult verified = mockMvc
                .perform(MockMvcRequestBuilders.post("/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + "\",\"code\":\"" + code + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        return JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
    }
}
