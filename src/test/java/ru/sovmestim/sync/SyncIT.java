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
    private static final String CURSOR_PARAM = "cursor";
    private static final String RESULTS_STATUS_PATH = "$.results[0].status";
    private static final String NEXT_CURSOR_PATH = "$.nextCursor";
    private static final String ALLERGY_DELETED_PATH = "$.allergies[0].deleted";
    private static final String ALLERGIES_LENGTH_PATH = "$.allergies.length()";
    private static final String CONDITION_ID_PATH = "$.conditions[0].id";
    private static final String CONDITION_NAME_PATH = "$.conditions[0].name";
    private static final String CONDITION_DELETED_PATH = "$.conditions[0].deleted";
    private static final String MEDICATIONS_LENGTH_PATH = "$.medications.length()";
    private static final String ADVICE_LENGTH_PATH = "$.advice.length()";
    private static final String ADVICE_STATUS_PATH = "$.advice[0].result.status";
    private static final String CODE_PATH = "$.code";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_CONFLICT = "CONFLICT";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_NO_INTERACTIONS = "NO_INTERACTIONS_REPORTED";
    private static final String STATUS_INTERACTION_FOUND = "INTERACTION_FOUND";
    private static final String LEVEL_AVOID = "AVOID";
    private static final String ERROR_ID_REQUIRED = "id is required";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String SECOND_IDEMPOTENCY_KEY = "med-2";
    private static final String ASPIRIN_CARDIO = "Аспирин Кардио";
    private static final String WARFARIN = "Варфарин";
    private static final String PENICILLIN = "пенициллин";
    private static final String HYPERTENSION = "гипертония";
    private static final String ICD10_HYPERTENSION = "I10";
    private static final String ACTIVE_STATUS = "активна";
    private static final String STALE_INSTANT = "2000-01-01T00:00:00Z";

    @Test
    void pushThenPullAllergyAndDeleteTombstone() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        UUID allergyId = UUID.randomUUID();

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"k1","allergies":[
                                  {"id":"%s","deleted":false,"name":"%s","severity":"средняя"}]}
                                """.formatted(allergyId, PENICILLIN)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));

        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.allergies[0].id").value(allergyId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.allergies[0].name").value(PENICILLIN))
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
    void pushConditionThenPullAndDeleteTombstone() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        UUID conditionId = UUID.randomUUID();

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"c1","conditions":[
                                  {"id":"%s","deleted":false,"name":"%s","mkbCode":"%s","status":"%s"}]}
                                """.formatted(conditionId, HYPERTENSION, ICD10_HYPERTENSION, ACTIVE_STATUS)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));

        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(CONDITION_ID_PATH).value(conditionId.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(CONDITION_NAME_PATH).value(HYPERTENSION))
                .andExpect(MockMvcResultMatchers.jsonPath(CONDITION_DELETED_PATH).value(false));

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"c2","conditions":[
                                  {"id":"%s","deleted":true}]}
                                """.formatted(conditionId)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));

        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(CONDITION_DELETED_PATH).value(true));
    }

    @Test
    void changeWithoutIdIsRejected() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"r1","allergies":[
                                  {"deleted":false,"name":"%s"}]}
                                """.formatted(PENICILLIN)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_REJECTED))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results[0].message").value(ERROR_ID_REQUIRED));
    }

    @Test
    void staleClientChangeIsReportedAsConflict() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        UUID medicationId = UUID.randomUUID();

        pushMedication(token, "s1", medicationId, ASPIRIN_CARDIO);

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"s2","medications":[
                                  {"id":"%s","deleted":false,"drugName":"%s","updatedAt":"%s"}]}
                                """.formatted(medicationId, ASPIRIN_CARDIO, STALE_INSTANT)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_CONFLICT));
    }

    @Test
    void pushWithUnknownMedicineIdIsNotFound() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"u1","medications":[
                                  {"id":"%s","deleted":false,"medicineId":"%s"}]}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(NOT_FOUND));
    }

    @Test
    void profileChangeTriggersRecheckOfActiveCourses() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        pushMedication(token, "p1", UUID.randomUUID(), WARFARIN);
        MvcResult firstPull = getMockMvc()
                .perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_LENGTH_PATH).value(1))
                .andReturn();
        String cursor = JsonPath.read(firstPull.getResponse().getContentAsString(), NEXT_CURSOR_PATH);

        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"p2","allergies":[
                                  {"id":"%s","deleted":false,"name":"%s"}]}
                                """.formatted(UUID.randomUUID(), PENICILLIN)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(RESULTS_STATUS_PATH).value(STATUS_APPLIED));

        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .param(CURSOR_PARAM, cursor))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ALLERGIES_LENGTH_PATH).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICATIONS_LENGTH_PATH).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_LENGTH_PATH).value(1));
    }

    @Test
    void medicationSyncTriggersServerSideAdviceAndIsIdempotent() throws Exception {
        String token = BEARER_PREFIX + loginAndGetToken();
        UUID warfarinId = UUID.randomUUID();
        UUID aspirinId = UUID.randomUUID();

        pushMedication(token, "med-1", warfarinId, WARFARIN);
        MvcResult firstPull = getMockMvc()
                .perform(MockMvcRequestBuilders.get(PULL_ENDPOINT).header(AUTHORIZATION_HEADER, token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_LENGTH_PATH).value(1))
                // The course being checked must not be reported as a duplicate of itself.
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_STATUS_PATH).value(STATUS_NO_INTERACTIONS))
                .andReturn();
        String cursor = JsonPath.read(firstPull.getResponse().getContentAsString(), NEXT_CURSOR_PATH);

        pushMedication(token, SECOND_IDEMPOTENCY_KEY, aspirinId, ASPIRIN_CARDIO);
        MvcResult secondPull = getMockMvc()
                .perform(MockMvcRequestBuilders.get(PULL_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .param(CURSOR_PARAM, cursor))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICATIONS_LENGTH_PATH).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_STATUS_PATH).value(STATUS_INTERACTION_FOUND))
                .andExpect(MockMvcResultMatchers.jsonPath("$.advice[0].result.level").value(LEVEL_AVOID))
                .andReturn();
        String nextCursor = JsonPath.read(secondPull.getResponse().getContentAsString(), NEXT_CURSOR_PATH);

        // Retrying the same idempotency key must not create new records or new advice.
        pushMedication(token, SECOND_IDEMPOTENCY_KEY, aspirinId, ASPIRIN_CARDIO);
        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT)
                        .header(AUTHORIZATION_HEADER, token)
                        .param(CURSOR_PARAM, nextCursor))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICATIONS_LENGTH_PATH).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(ADVICE_LENGTH_PATH).value(0));
    }

    @Test
    void syncEndpointsRequireAuthentication() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(PULL_ENDPOINT))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.post(PUSH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
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
