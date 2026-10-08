package ru.sovmestim.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.sovmestim.support.PostgresIntegrationTest;

class AdviceIT extends PostgresIntegrationTest {

    @Test
    void detectsDrugDrugInteractionForCurrentMedication() throws Exception {
        String token = bearer(loginAndGetToken());

        mockMvc.perform(post("/v1/medications")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Варфарин\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/advice/check")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Аспирин Кардио\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adviceId").isNotEmpty())
                .andExpect(jsonPath("$.result.status").value("INTERACTION_FOUND"))
                .andExpect(jsonPath("$.result.level").value("AVOID"))
                .andExpect(jsonPath("$.result.findings[0].kind").value("DRUG_DRUG"));
    }

    @Test
    void allergyDrivesForbiddenLevel() throws Exception {
        String token = bearer(loginAndGetToken());

        mockMvc.perform(post("/v1/profile/allergies")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ибупрофен\",\"severity\":\"тяжелая\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/advice/check")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Ибупрофен\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("INTERACTION_FOUND"))
                .andExpect(jsonPath("$.result.level").value("FORBIDDEN"))
                .andExpect(jsonPath("$.result.findings[?(@.kind == 'DRUG_ALLERGY')]").exists());
    }

    @Test
    void unresolvedDrugReturnsInsufficientDataNotError() throws Exception {
        String token = bearer(loginAndGetToken());

        mockMvc.perform(post("/v1/advice/check")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drugName\":\"Несуществующий препарат\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("INSUFFICIENT_DATA"));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
