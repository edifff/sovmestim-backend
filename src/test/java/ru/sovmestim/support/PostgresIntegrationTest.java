package ru.sovmestim.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL via Testcontainers. AGENTS.md requires database behavior to be tested against a
 * real database rather than mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTest {

    // Static so the container starts once and is shared across all context-cached test classes.
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sovmestim")
            .withUsername("sovmestim")
            .withPassword("sovmestim");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("sovmestim.demo.seed", () -> "true");
        registry.add("sovmestim.otp.debug-return-code", () -> "true");
    }

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Performs the full passwordless login and returns a bearer token.
     */
    protected String loginAndGetToken() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult requested = mockMvc
                .perform(post("/v1/auth/request-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
        String code = JsonPath.read(requested.getResponse().getContentAsString(), "$.devCode");

        MvcResult verified = mockMvc
                .perform(post("/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
    }
}
