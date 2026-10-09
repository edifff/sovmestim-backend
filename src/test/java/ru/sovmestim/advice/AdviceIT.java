package ru.sovmestim.advice;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the advice endpoints.
 */
class AdviceIT extends PostgresIntegrationTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHECK_ENDPOINT = "/v1/advice/check";
    private static final String RECHECK_ENDPOINT = "/v1/advice/recheck";
    private static final String MEDICATIONS_ENDPOINT = "/v1/medications";
    private static final String SUBSTANCES_ENDPOINT = "/v1/catalog/substances";
    private static final String RESOLVE_ENDPOINT = "/v1/catalog/resolve";
    private static final String NAME_PARAM = "name";
    private static final String QUERY_PARAM = "query";
    private static final String DRUG_NAME_BODY = """
            {"drugName":"%s"}
            """;
    private static final String MEDICINE_ID_BODY = """
            {"medicineId":"%s"}
            """;
    private static final String SUBSTANCE_IDS_BODY = """
            {"substanceIds":["%s"]}
            """;
    private static final String EMPTY_BODY = "{}";
    private static final String ADVICE_ID_PATH = "$.adviceId";
    private static final String MEDICINE_ID_PATH = "$.medicineId";
    private static final String SUBSTANCE_ID_PATH = "$[0].id";
    private static final String CODE_PATH = "$.code";
    private static final String RESULT_STATUS_PATH = "$.result.status";
    private static final String RESULT_LEVEL_PATH = "$.result.level";
    private static final String STATUS_INTERACTION_FOUND = "INTERACTION_FOUND";
    private static final String STATUS_INSUFFICIENT_DATA = "INSUFFICIENT_DATA";
    private static final String STATUS_NO_INTERACTIONS = "NO_INTERACTIONS_REPORTED";
    private static final String LEVEL_AVOID = "AVOID";
    private static final String LEVEL_FORBIDDEN = "FORBIDDEN";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String BAD_REQUEST = "BAD_REQUEST";
    private static final String WARFARIN = "Варфарин";
    private static final String WARFARIN_INN = "варфарин";
    private static final String ASPIRIN_CARDIO = "Аспирин Кардио";
    private static final String IBUPROFEN = "Ибупрофен";
    private static final String SEVERE = "тяжелая";
    private static final String UNKNOWN_DRUG = "Несуществующий препарат";

    @Test
    void detectsDrugDrugInteractionForCurrentMedication() throws Exception {
        String token = bearer(loginAndGetToken());
        addMedication(token, WARFARIN);

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRUG_NAME_BODY.formatted(ASPIRIN_CARDIO)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_ID_PATH).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_INTERACTION_FOUND))
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_LEVEL_PATH).value(LEVEL_AVOID))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.findings[0].kind").value("DRUG_DRUG"));
    }

    @Test
    void allergyDrivesForbiddenLevel() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post("/v1/profile/allergies")
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ибупрофен","severity":"%s"}
                                """.formatted(SEVERE)))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRUG_NAME_BODY.formatted(IBUPROFEN)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_INTERACTION_FOUND))
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_LEVEL_PATH).value(LEVEL_FORBIDDEN))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.findings[?(@.kind == 'DRUG_ALLERGY')]").exists());
    }

    @Test
    void checkByCatalogMedicineIdReturnsResult() throws Exception {
        String token = bearer(loginAndGetToken());
        String medicineId = resolveMedicineId(WARFARIN);

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MEDICINE_ID_BODY.formatted(medicineId)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_ID_PATH).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_NO_INTERACTIONS));
    }

    @Test
    void checkBySubstanceIdsReturnsResult() throws Exception {
        String token = bearer(loginAndGetToken());
        String substanceId = resolveSubstanceId(WARFARIN_INN);

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBSTANCE_IDS_BODY.formatted(substanceId)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_ID_PATH).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_NO_INTERACTIONS));
    }

    @Test
    void checkWithoutIdentifiersIsBadRequest() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_BODY))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(BAD_REQUEST));
    }

    @Test
    void checkWithUnknownMedicineIdIsNotFound() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MEDICINE_ID_BODY.formatted(UUID.randomUUID())))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(NOT_FOUND));
    }

    @Test
    void unresolvedDrugReturnsInsufficientDataNotError() throws Exception {
        String token = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRUG_NAME_BODY.formatted(UNKNOWN_DRUG)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULT_STATUS_PATH).value(STATUS_INSUFFICIENT_DATA));
    }

    @Test
    void recheckRecomputesEveryActiveCourse() throws Exception {
        String token = bearer(loginAndGetToken());
        addMedication(token, WARFARIN);

        getMockMvc().perform(MockMvcRequestBuilders.post(RECHECK_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].adviceId").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].result.status").value(STATUS_NO_INTERACTIONS));
    }

    @Test
    void adviceEndpointsRequireAuthentication() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.post(CHECK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_BODY))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private void addMedication(String token, String drugName) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.post(MEDICATIONS_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRUG_NAME_BODY.formatted(drugName)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    private String resolveMedicineId(String name) throws Exception {
        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(RESOLVE_ENDPOINT).param(NAME_PARAM, name))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICINE_ID_PATH).isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), MEDICINE_ID_PATH);
    }

    private String resolveSubstanceId(String name) throws Exception {
        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(SUBSTANCES_ENDPOINT).param(QUERY_PARAM, name))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(SUBSTANCE_ID_PATH).isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), SUBSTANCE_ID_PATH);
    }

    private static String bearer(String token) {
        return BEARER_PREFIX + token;
    }
}
