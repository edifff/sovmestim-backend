package ru.sovmestim.sync;

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

class SyncIT extends PostgresIntegrationTest {

    @Test
    void pushThenPullAllergyAndDeleteTombstone() throws Exception {
        String token = "Bearer " + loginAndGetToken();
        UUID allergyId = UUID.randomUUID();

        mockMvc.perform(post("/v1/sync/push")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"k1","allergies":[
                                  {"id":"%s","deleted":false,"name":"пенициллин","severity":"средняя"}]}
                                """.formatted(allergyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"));

        mockMvc.perform(get("/v1/sync/pull").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies[0].id").value(allergyId.toString()))
                .andExpect(jsonPath("$.allergies[0].name").value("пенициллин"))
                .andExpect(jsonPath("$.allergies[0].deleted").value(false))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty());

        mockMvc.perform(post("/v1/sync/push")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"k2","allergies":[
                                  {"id":"%s","deleted":true}]}
                                """.formatted(allergyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"));

        mockMvc.perform(get("/v1/sync/pull").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies[0].deleted").value(true));
    }

    @Test
    void medicationSyncTriggersServerSideAdviceAndIsIdempotent() throws Exception {
        String token = "Bearer " + loginAndGetToken();
        UUID warfarinId = UUID.randomUUID();
        UUID aspirinId = UUID.randomUUID();

        pushMedication(token, "med-1", warfarinId, "Варфарин");
        MvcResult firstPull = mockMvc
                .perform(get("/v1/sync/pull").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advice.length()").value(1))
                // The course being checked must not be reported as a duplicate of itself.
                .andExpect(jsonPath("$.advice[0].result.status").value("NO_INTERACTIONS_REPORTED"))
                .andReturn();
        String cursor = JsonPath.read(firstPull.getResponse().getContentAsString(), "$.nextCursor");

        pushMedication(token, "med-2", aspirinId, "Аспирин Кардио");
        MvcResult secondPull = mockMvc
                .perform(get("/v1/sync/pull").header("Authorization", token).param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medications.length()").value(1))
                .andExpect(jsonPath("$.advice[0].result.status").value("INTERACTION_FOUND"))
                .andExpect(jsonPath("$.advice[0].result.level").value("AVOID"))
                .andReturn();
        String nextCursor = JsonPath.read(secondPull.getResponse().getContentAsString(), "$.nextCursor");

        // Retrying the same idempotency key must not create new records or new advice.
        pushMedication(token, "med-2", aspirinId, "Аспирин Кардио");
        mockMvc.perform(get("/v1/sync/pull").header("Authorization", token).param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medications.length()").value(0))
                .andExpect(jsonPath("$.advice.length()").value(0));
    }

    private void pushMedication(String token, String key, UUID id, String drugName) throws Exception {
        mockMvc.perform(post("/v1/sync/push")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"%s","medications":[
                                  {"id":"%s","deleted":false,"drugName":"%s"}]}
                                """.formatted(key, id, drugName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("APPLIED"));
    }
}
