package ru.sovmestim.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import ru.sovmestim.support.PostgresIntegrationTest;

class AuthFlowIT extends PostgresIntegrationTest {

    @Test
    void loginRefreshAndAccessProtectedResource() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        MvcResult requested = mockMvc
                .perform(post("/v1/auth/request-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.devCode").isNotEmpty())
                .andReturn();
        String code = JsonPath.read(requested.getResponse().getContentAsString(), "$.devCode");

        MvcResult verified = mockMvc
                .perform(post("/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String accessToken = JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
        String refreshToken = JsonPath.read(verified.getResponse().getContentAsString(), "$.refreshToken");

        mockMvc.perform(get("/v1/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/v1/profile")).andExpect(status().isUnauthorized());

        MvcResult refreshed = mockMvc
                .perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String rotated = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.accessToken");
        assertThat(rotated).isNotBlank();

        // A refresh token is single-use: replaying the old one must fail.
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongCodeIsRejected() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/v1/auth/request-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));

        mockMvc.perform(post("/v1/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }
}
