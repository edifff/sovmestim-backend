package ru.sovmestim.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Validates real requests and responses from the running application against {@code openapi.yaml}.
 * Any response shape or status that drifts from the contract fails the build.
 */
class OpenApiValidationIT extends PostgresIntegrationTest {

    private OpenApiInteractionValidator validator;

    @BeforeEach
    void setUpValidator() {
        validator = OpenApiInteractionValidator
                .createFor(OpenApiSpec.file().toAbsolutePath().toString())
                .build();
    }

    @Test
    void documentedFlowsConformToTheContract() throws Exception {
        String email = "contract-" + UUID.randomUUID() + "@example.com";

        // --- Auth ---------------------------------------------------------
        String requestCodeBody = "{\"email\":\"" + email + "\"}";
        MvcResult requested = call(post("/v1/auth/request-code").contentType(MediaType.APPLICATION_JSON)
                .content(requestCodeBody), null);
        validate("POST", "/v1/auth/request-code", null, null, requestCodeBody, requested);
        String code = JsonPath.read(requested.getResponse().getContentAsString(), "$.devCode");

        String verifyBody = "{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}";
        MvcResult verified = call(post("/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content(verifyBody), null);
        validate("POST", "/v1/auth/verify", null, null, verifyBody, verified);
        String token = JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
        String refreshToken = JsonPath.read(verified.getResponse().getContentAsString(), "$.refreshToken");

        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        MvcResult refreshed = call(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody), null);
        validate("POST", "/v1/auth/refresh", null, null, refreshBody, refreshed);

        // --- Catalog ------------------------------------------------------
        MvcResult substances = call(get("/v1/catalog/substances").param("query", "варф"), null);
        validate("GET", "/v1/catalog/substances", Map.of("query", "варф"), null, null, substances);

        MvcResult medicines = call(get("/v1/catalog/medicines").param("query", "Аспирин"), null);
        validate("GET", "/v1/catalog/medicines", Map.of("query", "Аспирин"), null, null, medicines);
        String medicineId = JsonPath.read(medicines.getResponse().getContentAsString(), "$[0].id");

        MvcResult medicine = call(get("/v1/catalog/medicines/" + medicineId), null);
        validate("GET", "/v1/catalog/medicines/" + medicineId, null, null, null, medicine);

        MvcResult resolved = call(get("/v1/catalog/resolve").param("name", "Аспирин Кардио"), null);
        validate("GET", "/v1/catalog/resolve", Map.of("name", "Аспирин Кардио"), null, null, resolved);

        // --- Profile ------------------------------------------------------
        MvcResult profile = call(get("/v1/profile"), token);
        validate("GET", "/v1/profile", null, token, null, profile);

        String allergyBody = "{\"name\":\"ибупрофен\",\"severity\":\"средняя\"}";
        MvcResult allergyCreated = call(post("/v1/profile/allergies").contentType(MediaType.APPLICATION_JSON)
                .content(allergyBody), token);
        validate("POST", "/v1/profile/allergies", null, token, allergyBody, allergyCreated);
        String allergyId = JsonPath.read(allergyCreated.getResponse().getContentAsString(), "$.id");

        MvcResult allergyList = call(get("/v1/profile/allergies"), token);
        validate("GET", "/v1/profile/allergies", null, token, null, allergyList);

        String allergyUpdateBody = "{\"name\":\"ибупрофен\",\"severity\":\"тяжелая\"}";
        MvcResult allergyUpdated = call(put("/v1/profile/allergies/" + allergyId)
                .contentType(MediaType.APPLICATION_JSON).content(allergyUpdateBody), token);
        validate("PUT", "/v1/profile/allergies/" + allergyId, null, token, allergyUpdateBody, allergyUpdated);

        MvcResult allergyDeleted = call(delete("/v1/profile/allergies/" + allergyId), token);
        validate("DELETE", "/v1/profile/allergies/" + allergyId, null, token, null, allergyDeleted);

        String conditionBody = "{\"name\":\"Гипертония\",\"mkbCode\":\"I10\"}";
        MvcResult conditionCreated = call(post("/v1/profile/conditions").contentType(MediaType.APPLICATION_JSON)
                .content(conditionBody), token);
        validate("POST", "/v1/profile/conditions", null, token, conditionBody, conditionCreated);

        MvcResult conditionList = call(get("/v1/profile/conditions"), token);
        validate("GET", "/v1/profile/conditions", null, token, null, conditionList);

        // --- Medications --------------------------------------------------
        String medicationBody = "{\"drugName\":\"Варфарин\",\"dosage\":\"2.5 мг\"}";
        MvcResult medicationCreated = call(post("/v1/medications").contentType(MediaType.APPLICATION_JSON)
                .content(medicationBody), token);
        validate("POST", "/v1/medications", null, token, medicationBody, medicationCreated);

        MvcResult medicationList = call(get("/v1/medications"), token);
        validate("GET", "/v1/medications", null, token, null, medicationList);

        // --- Advice -------------------------------------------------------
        String adviceBody = "{\"drugName\":\"Аспирин Кардио\"}";
        MvcResult advice = call(post("/v1/advice/check").contentType(MediaType.APPLICATION_JSON).content(adviceBody), token);
        validate("POST", "/v1/advice/check", null, token, adviceBody, advice);

        MvcResult recheck = call(post("/v1/advice/recheck"), token);
        validate("POST", "/v1/advice/recheck", null, token, null, recheck);

        // --- Sync ---------------------------------------------------------
        String syncBody = """
                {"idempotencyKey":"contract","allergies":[
                  {"id":"%s","deleted":false,"name":"пенициллин","severity":"легкая"}],
                 "medications":[
                  {"id":"%s","deleted":false,"drugName":"Варфарин"}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        MvcResult pushed = call(post("/v1/sync/push").contentType(MediaType.APPLICATION_JSON).content(syncBody), token);
        validate("POST", "/v1/sync/push", null, token, syncBody, pushed);

        MvcResult pulled = call(get("/v1/sync/pull"), token);
        validate("GET", "/v1/sync/pull", null, token, null, pulled);
    }

    private MvcResult call(MockHttpServletRequestBuilder builder, String token) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(builder).andReturn();
    }

    private void validate(
            String method,
            String path,
            Map<String, String> query,
            String token,
            String requestBody,
            MvcResult result)
            throws Exception {
        SimpleRequest.Builder request = switch (method) {
            case "GET" -> SimpleRequest.Builder.get(path);
            case "POST" -> SimpleRequest.Builder.post(path);
            case "PUT" -> SimpleRequest.Builder.put(path);
            case "DELETE" -> SimpleRequest.Builder.delete(path);
            default -> throw new IllegalArgumentException("Unsupported method " + method);
        };
        if (query != null) {
            query.forEach(request::withQueryParam);
        }
        if (token != null) {
            request.withAuthorization("Bearer " + token);
        }
        if (requestBody != null) {
            request.withContentType("application/json").withBody(requestBody);
        }

        SimpleResponse.Builder response = SimpleResponse.Builder.status(result.getResponse().getStatus());
        String responseBody = result.getResponse().getContentAsString();
        if (!responseBody.isBlank()) {
            response.withContentType("application/json").withBody(responseBody);
        }

        ValidationReport report = validator.validate(request.build(), response.build());
        List<String> errors = report.getMessages().stream()
                .filter(message -> message.getLevel() == ValidationReport.Level.ERROR)
                .map(message -> message.getKey() + ": " + message.getMessage())
                .toList();
        assertThat(errors).as("Contract violations for %s %s", method, path).isEmpty();
    }
}
