package ru.sovmestim.sync;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the synchronization endpoints.
 */
class SyncIT extends PostgresIntegrationTest {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PUSH_ENDPOINT = "/v1/sync/push";
    private static final String PULL_ENDPOINT = "/v1/sync/pull";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String RESULTS_STATUS_PATH = "$.results[0].status";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String ALLERGY_DELETED_PATH = "$.allergies[0].deleted";
    private static final String NEXT_CURSOR_PATH = "$.nextCursor";
    private static final String ADVICE_LENGTH_PATH = "$.advice.length()";
    private static final String ADVICE_STATUS_PATH = "$.advice[0].result.status";
    private static final String MEDICATIONS_LENGTH_PATH = "$.medications.length()";
    private static final String CURSOR_PARAM = "cursor";
    private static final String SECOND_IDEMPOTENCY_KEY = "med-2";
    private static final String ASPIRIN_CARDIO = "Аспирин Кардио";

    @Test
    void pushThenPullAllergyAndDeleteTombstone() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        UUID allergyId = UUID.randomUUID();

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"k1","allergies":[
                                  {"id":"%s","deleted":false,"name":"пенициллин","severity":"средняя"}]}
                                """.formatted(allergyId)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));

        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.allergies[0].id").value(allergyId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.allergies[0].name").value("пенициллин"))
                .andExpect(MockMvcResultMatchers.jsonPath(ALLERGY_DELETED_PATH).value(false))
                .andExpect(MockMvcResultMatchers.jsonPath(NEXT_CURSOR_PATH).isNotEmpty());

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"k2","allergies":[
                                  {"id":"%s","deleted":true}]}
                                """.formatted(allergyId)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));

        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ALLERGY_DELETED_PATH).value(true));
    }

    @Test
    void medicationSyncTriggersServerSideAdviceAndIsIdempotent() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        UUID warfarinId = UUID.randomUUID();
        UUID aspirinId = UUID.randomUUID();

        pushMedication(token, "med-1", warfarinId, "Варфарин");
        MvcResult firstPull = getMockMvc()
                .perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_LENGTH_PATH).value(1))
                // The course being checked must not be reported as a duplicate of itself.
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_STATUS_PATH).value("NO_INTERACTIONS_REPORTED"))
                .andReturn();
        String cursor = JsonPath.read(firstPull.getResponse().getContentAsString(), NEXT_CURSOR_PATH);

        pushMedication(token, SECOND_IDEMPOTENCY_KEY, aspirinId, ASPIRIN_CARDIO);
        MvcResult secondPull = getMockMvc()
                .perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token).param(CURSOR_PARAM, cursor))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICATIONS_LENGTH_PATH).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_STATUS_PATH).value("INTERACTION_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.advice[0].result.level").value("AVOID"))
                .andReturn();
        String nextCursor = JsonPath.read(secondPull.getResponse().getContentAsString(), NEXT_CURSOR_PATH);

        // Retrying the same idempotency key must not create new records or new advice.
        pushMedication(token, SECOND_IDEMPOTENCY_KEY, aspirinId, ASPIRIN_CARDIO);
        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token).param(CURSOR_PARAM, nextCursor))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICATIONS_LENGTH_PATH).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_LENGTH_PATH).value(0));
    }

    private void pushMedication(String token, String key, UUID id, String drugName) throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s","medications":[
                                  {"id":"%s","deleted":false,"drugName":"%s"}]}
                                """.formatted(key, id, drugName)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));
    }
}
