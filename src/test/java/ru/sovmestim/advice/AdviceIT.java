package ru.sovmestim.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the advice endpoints.
 */
class AdviceIT extends PostgresIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CHECK_ENDPOINT = "/v1/advice/check";
    private static final String RESULT_STATUS_PATH = "$.result.status";
    private static final String RESULT_LEVEL_PATH = "$.result.level";
    private static final String STATUS_INTERACTION_FOUND = "INTERACTION_FOUND";

    @Test
    void detectsDrugDrugInteractionForCurrentMedication() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post("/v1/medications")
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Варфарин\"}"))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Аспирин Кардио\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.adviceId").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_INTERACTION_FOUND))
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_LEVEL_PATH).value("AVOID"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.findings[0].kind").value("DRUG_DRUG"));
    }

    @Test
    void allergyDrivesForbiddenLevel() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post("/v1/profile/allergies")
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ибупрофен\",\"severity\":\"тяжелая\"}"))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Ибупрофен\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_INTERACTION_FOUND))
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_LEVEL_PATH).value("FORBIDDEN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.findings[?(@.kind == 'DRUG_ALLERGY')]").exists());
    }

    @Test
    void unresolvedDrugReturnsInsufficientDataNotError() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Несуществующий препарат\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value("INSUFFICIENT_DATA"));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
