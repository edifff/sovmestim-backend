package ru.sovmestim.identity;

import java.util.Locale;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
    private static final String EMAIL_JSON_PATH = "$.email";
    private static final String CODE_PATH = "$.code";
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String BAD_REQUEST = "BAD_REQUEST";
    private static final String INVALID_EMAIL = "not-an-email";
    private static final String UNKNOWN_REFRESH_TOKEN = "unknown-refresh-token";
    private static final String WRONG_CODE = "111111";
    private static final String TOO_MANY_MESSAGE = "Too many attempts";
    private static final int MAX_OTP_ATTEMPTS = 5;

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
                .andExpect(MockMvcResultMatchers.jsonPath(EMAIL_JSON_PATH).value(email));

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

    @Test
    void requestCodeNormalizesEmail() throws Exception {
        String email = "User-" + UUID.randomUUID() + "@Example.COM";

        getMockMvc().perform(MockMvcRequestBuilders.post(REQUEST_CODE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andExpect(MockMvcResultMatchers.jsonPath(EMAIL_JSON_PATH).value(email.toLowerCase(Locale.ROOT)));
    }

    @Test
    void requestCodeRejectsMalformedEmail() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.post(REQUEST_CODE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + INVALID_EMAIL + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(VALIDATION_ERROR));
    }

    @Test
    void verifyWithoutRequestedCodeIsRejected() throws Exception {
        String email = TEST_USER_PREFIX + UUID.randomUUID() + TEST_EMAIL_DOMAIN;

        getMockMvc().perform(MockMvcRequestBuilders.post(VERIFY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_EMAIL_PREFIX + email + "\",\"code\":\"999999" + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(BAD_REQUEST));
    }

    @Test
    void refreshWithUnknownTokenIsRejected() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.post(REFRESH_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_REFRESH_TOKEN_PREFIX + UNKNOWN_REFRESH_TOKEN + JSON_BODY_SUFFIX))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(BAD_REQUEST));
    }

    @Test
    void tooManyWrongAttemptsLocksTheCode() throws Exception {
        String email = TEST_USER_PREFIX + UUID.randomUUID() + TEST_EMAIL_DOMAIN;
        requestLoginCode(email);

        for (int attempt = 0; attempt < MAX_OTP_ATTEMPTS; attempt++) {
            verifyCode(email, WRONG_CODE).andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        verifyCode(email, WRONG_CODE)
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value(Matchers.containsString(TOO_MANY_MESSAGE)));
    }

    private MvcResult requestLoginCode(String email) throws Exception {
        return getMockMvc()
                .perform(MockMvcRequestBuilders.post(REQUEST_CODE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(MockMvcResultMatchers.status().isAccepted())
                .andReturn();
    }

    private ResultActions verifyCode(String email, String code) throws Exception {
        return getMockMvc().perform(MockMvcRequestBuilders.post(VERIFY_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","code":"%s"}
                        """.formatted(email, code)));
    }
}
