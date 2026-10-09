package ru.sovmestim;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Verifies the security boundary: the catalog and health endpoints are public, everything else
 * requires a valid bearer token.
 */
class SecurityIT extends PostgresIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CATALOG_SUBSTANCES_PATH = "/v1/catalog/substances";
    private static final String PROFILE_PATH = "/v1/profile";
    private static final String MEDICATIONS_PATH = "/v1/medications";
    private static final String ADVICE_CHECK_PATH = "/v1/advice/check";
    private static final String SYNC_PULL_PATH = "/v1/sync/pull";
    private static final String HEALTH_PATH = "/actuator/health";
    private static final String QUERY_PARAM = "query";
    private static final String SUBSTANCE_QUERY = "варфарин";
    private static final String INVALID_TOKEN = "not-a-valid-token";

    @Test
    void catalogIsReachableWithoutToken() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(CATALOG_SUBSTANCES_PATH).param(QUERY_PARAM, SUBSTANCE_QUERY))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value(SUBSTANCE_QUERY));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(HEALTH_PATH))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void protectedEndpointsRejectAnonymousRequests() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(PROFILE_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICATIONS_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.get(SYNC_PULL_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.post(ADVICE_CHECK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void malformedBearerTokenIsRejected() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(PROFILE_PATH)
                        .header(AUTHORIZATION, BEARER_PREFIX + INVALID_TOKEN))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
