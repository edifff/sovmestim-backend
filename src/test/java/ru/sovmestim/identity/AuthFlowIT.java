package ru.sovmestim.identity;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the passwordless authentication flow.
 */
class AuthFlowIT extends PostgresIntegrationTest {

    private static final String TEST_USER_PREFIX = "user-";
    private static final String TEST_EMAIL_DOMAIN = "@example.com";

    private static final String REQUEST_CODE_PATH = "/v1/auth/request-code";
    private static final String VERIFY_PATH = "/v1/auth/verify";
    private static final String REFRESH_PATH = "/v1/auth/refresh";
    private static final String PROFILE_PATH = "/v1/profile";

    private static final String DEV_CODE_JSON_PATH = "$.devCode";
    private static final String ACCESS_TOKEN_JSON_PATH = "$.accessToken";

    private static final String JSON_EMAIL_PREFIX = "{\"email\":\"";
    private static final String JSON_REFRESH_TOKEN_PREFIX = "{\"refreshToken\":\"";
    private static final String JSON_BODY_SUFFIX = "\"}";

    @Test
    void loginRefreshAndAccessProtectedResource() throws Exception {
        String email = TEST_USER_PREFIX + UUID.randomUUID() + TEST_EMAIL_DOMAIN;

        MvcResult requested = getMockMvc()
                .perform(MockMvcRequestBuilders.post(REQUEST_CODE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andExpect(MockMvcResultMatchers.jsonPath(DEV_CODE_JSON_PATH).isNotEmpty())
                .andReturn();
        String code = JsonPath.read(requested.getResponse().getContentAsString(), DEV_CODE_JSON_PATH);

        MvcResult verified = getMockMvc()
                .perform(MockMvcRequestBuilders.post(VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + "\",\"code\":\"" + code + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ACCESS_TOKEN_JSON_PATH).isNotEmpty())
                .andReturn();
        String accessToken = JsonPath.read(verified.getResponse().getContentAsString(), ACCESS_TOKEN_JSON_PATH);
        String refreshToken = JsonPath.read(verified.getResponse().getContentAsString(), "$.refreshToken");

        getMockMvc().perform(MockMvcRequestBuilders.get(PROFILE_PATH).header("Authorization", "Bearer " + accessToken))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value(email));

        getMockMvc().perform(MockMvcRequestBuilders.get(PROFILE_PATH)).andExpect(MockMvcResultMatchers.status().isUnauthorized());

        MvcResult refreshed = getMockMvc()
                .perform(MockMvcRequestBuilders.post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REFRESH_TOKEN_PREFIX + refreshToken + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String rotated = JsonPath.read(refreshed.getResponse().getContentAsString(), ACCESS_TOKEN_JSON_PATH);
        Assertions.assertThat(rotated).isNotBlank();

        // A refresh token is single-use: replaying the old one must fail.
        getMockMvc().perform(MockMvcRequestBuilders.post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REFRESH_TOKEN_PREFIX + refreshToken + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void wrongCodeIsRejected() throws Exception {
        String email = TEST_USER_PREFIX + UUID.randomUUID() + TEST_EMAIL_DOMAIN;
        getMockMvc().perform(MockMvcRequestBuilders.post(REQUEST_CODE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON_EMAIL_PREFIX + email + JSON_BODY_SUFFIX));

        getMockMvc().perform(MockMvcRequestBuilders.post(VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + "\",\"code\":\"000000" + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
