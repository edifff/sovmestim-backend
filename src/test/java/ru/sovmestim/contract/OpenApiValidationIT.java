package ru.sovmestim.contract;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.jayway.jsonpath.JsonPath;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Validates real requests and responses from the running application against {@code openapi.yaml}.
 * Any response shape or status that drifts from the contract fails the build.
 */
class OpenApiValidationIT extends PostgresIntegrationTest {

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";

    private static final String AUTH_REQUEST_CODE_PATH = "/v1/auth/request-code";
    private static final String AUTH_VERIFY_PATH = "/v1/auth/verify";
    private static final String AUTH_REFRESH_PATH = "/v1/auth/refresh";
    private static final String CATALOG_SUBSTANCES_PATH = "/v1/catalog/substances";
    private static final String CATALOG_MEDICINES_PATH = "/v1/catalog/medicines";
    private static final String CATALOG_MEDICINE_BY_ID_PATH = "/v1/catalog/medicines/";
    private static final String CATALOG_RESOLVE_PATH = "/v1/catalog/resolve";
    private static final String PROFILE_PATH = "/v1/profile";
    private static final String PROFILE_ALLERGIES_PATH = "/v1/profile/allergies";
    private static final String PROFILE_ALLERGY_BY_ID_PATH = "/v1/profile/allergies/";
    private static final String PROFILE_CONDITIONS_PATH = "/v1/profile/conditions";
    private static final String MEDICATIONS_PATH = "/v1/medications";
    private static final String ADVICE_CHECK_PATH = "/v1/advice/check";
    private static final String ADVICE_RECHECK_PATH = "/v1/advice/recheck";
    private static final String SYNC_PUSH_PATH = "/v1/sync/push";
    private static final String SYNC_PULL_PATH = "/v1/sync/pull";

    private static final String QUERY_PARAM = "query";
    private static final String NAME_PARAM = "name";

    private static final String WARFARIN_QUERY = "варф";
    private static final String ASPIRIN_MEDICINE = "Аспирин";
    private static final String ASPIRIN_CARDIO_MEDICINE = "Аспирин Кардио";

    private static final String JSON_EMAIL_PREFIX = "{\"email\":\"";
    private static final String JSON_BODY_SUFFIX = "\"}";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private OpenApiInteractionValidator validator;

    @BeforeEach
    void setUpValidator() {
        validator = OpenApiInteractionValidator
                .createFor(OpenApiSpec.file().toAbsolutePath().toString())
                .build();
    }

    @Test
    void documentedFlowsConformToTheContract() throws Exception {
        String token = validateAuthFlows();
        validateCatalogFlows();
        validateProfileFlows(token);
        validateMedicationFlows(token);
        validateAdviceFlows(token);
        validateSyncFlows(token);
    }

    private String validateAuthFlows() throws Exception {
        String email = "contract-" + UUID.randomUUID() + "@example.com";

        String requestCodeBody = JSON_EMAIL_PREFIX + email + JSON_BODY_SUFFIX;
        MvcResult requested = call(MockMvcRequestBuilders.post(AUTH_REQUEST_CODE_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(requestCodeBody), null);
        validate(METHOD_POST, AUTH_REQUEST_CODE_PATH, null, null, requestCodeBody, requested);
        String code = JsonPath.read(requested.getResponse().getContentAsString(), "$.devCode");

        String verifyBody = JSON_EMAIL_PREFIX + email + "\",\"code\":\"" + code + JSON_BODY_SUFFIX;
        MvcResult verified = call(MockMvcRequestBuilders.post(AUTH_VERIFY_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(verifyBody), null);
        validate(METHOD_POST, AUTH_VERIFY_PATH, null, null, verifyBody, verified);
        String token = JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
        String refreshToken = JsonPath.read(verified.getResponse().getContentAsString(), "$.refreshToken");

        String refreshBody = "{\"refreshToken\":\"" + refreshToken + JSON_BODY_SUFFIX;
        MvcResult refreshed = call(MockMvcRequestBuilders.post(AUTH_REFRESH_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody), null);
        validate(METHOD_POST, AUTH_REFRESH_PATH, null, null, refreshBody, refreshed);
        return token;
    }

    private void validateCatalogFlows() throws Exception {
        MvcResult substances = call(MockMvcRequestBuilders.get(CATALOG_SUBSTANCES_PATH)
                .param(QUERY_PARAM, WARFARIN_QUERY), null);
        validate(METHOD_GET, CATALOG_SUBSTANCES_PATH, Map.of(QUERY_PARAM, WARFARIN_QUERY), null, null, substances);

        MvcResult medicines = call(MockMvcRequestBuilders.get(CATALOG_MEDICINES_PATH)
                .param(QUERY_PARAM, ASPIRIN_MEDICINE), null);
        validate(METHOD_GET, CATALOG_MEDICINES_PATH, Map.of(QUERY_PARAM, ASPIRIN_MEDICINE), null, null, medicines);
        String medicineId = JsonPath.read(medicines.getResponse().getContentAsString(), "$[0].id");

        MvcResult medicine = call(MockMvcRequestBuilders.get(CATALOG_MEDICINE_BY_ID_PATH + medicineId), null);
        validate(METHOD_GET, CATALOG_MEDICINE_BY_ID_PATH + medicineId, null, null, null, medicine);

        MvcResult resolved = call(MockMvcRequestBuilders.get(CATALOG_RESOLVE_PATH)
                .param(NAME_PARAM, ASPIRIN_CARDIO_MEDICINE), null);
        validate(METHOD_GET, CATALOG_RESOLVE_PATH, Map.of(NAME_PARAM, ASPIRIN_CARDIO_MEDICINE), null, null, resolved);
    }

    private void validateProfileFlows(String token) throws Exception {
        MvcResult profile = call(MockMvcRequestBuilders.get(PROFILE_PATH), token);
        validate(METHOD_GET, PROFILE_PATH, null, token, null, profile);

        String allergyBody = "{\"name\":\"ибупрофен\",\"severity\":\"средняя\"}";
        MvcResult allergyCreated = call(MockMvcRequestBuilders.post(PROFILE_ALLERGIES_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(allergyBody), token);
        validate(METHOD_POST, PROFILE_ALLERGIES_PATH, null, token, allergyBody, allergyCreated);
        String allergyId = JsonPath.read(allergyCreated.getResponse().getContentAsString(), "$.id");

        MvcResult allergyList = call(MockMvcRequestBuilders.get(PROFILE_ALLERGIES_PATH), token);
        validate(METHOD_GET, PROFILE_ALLERGIES_PATH, null, token, null, allergyList);

        String allergyUpdateBody = "{\"name\":\"ибупрофен\",\"severity\":\"тяжелая\"}";
        MvcResult allergyUpdated = call(MockMvcRequestBuilders.put(PROFILE_ALLERGY_BY_ID_PATH + allergyId)
                .contentType(MediaType.APPLICATION_JSON).content(allergyUpdateBody), token);
        validate(METHOD_PUT, PROFILE_ALLERGY_BY_ID_PATH + allergyId, null, token, allergyUpdateBody, allergyUpdated);

        MvcResult allergyDeleted = call(MockMvcRequestBuilders.delete(PROFILE_ALLERGY_BY_ID_PATH + allergyId), token);
        validate(METHOD_DELETE, PROFILE_ALLERGY_BY_ID_PATH + allergyId, null, token, null, allergyDeleted);

        String conditionBody = "{\"name\":\"Гипертония\",\"mkbCode\":\"I10\"}";
        MvcResult conditionCreated = call(MockMvcRequestBuilders.post(PROFILE_CONDITIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(conditionBody), token);
        validate(METHOD_POST, PROFILE_CONDITIONS_PATH, null, token, conditionBody, conditionCreated);

        MvcResult conditionList = call(MockMvcRequestBuilders.get(PROFILE_CONDITIONS_PATH), token);
        validate(METHOD_GET, PROFILE_CONDITIONS_PATH, null, token, null, conditionList);
    }

    private void validateMedicationFlows(String token) throws Exception {
        String medicationBody = "{\"drugName\":\"Варфарин\",\"dosage\":\"2.5 мг\"}";
        MvcResult medicationCreated = call(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(medicationBody), token);
        validate(METHOD_POST, MEDICATIONS_PATH, null, token, medicationBody, medicationCreated);

        MvcResult medicationList = call(MockMvcRequestBuilders.get(MEDICATIONS_PATH), token);
        validate(METHOD_GET, MEDICATIONS_PATH, null, token, null, medicationList);
    }

    private void validateAdviceFlows(String token) throws Exception {
        String adviceBody = "{\"drugName\":\"Аспирин Кардио\"}";
        MvcResult advice = call(MockMvcRequestBuilders.post(ADVICE_CHECK_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(adviceBody), token);
        validate(METHOD_POST, ADVICE_CHECK_PATH, null, token, adviceBody, advice);

        MvcResult recheck = call(MockMvcRequestBuilders.post(ADVICE_RECHECK_PATH), token);
        validate(METHOD_POST, ADVICE_RECHECK_PATH, null, token, null, recheck);
    }

    private void validateSyncFlows(String token) throws Exception {
        String syncBody = """
                {"idempotencyKey":"contract","allergies":[
                  {"id":"%s","deleted":false,"name":"пенициллин","severity":"легкая"}],
                 "medications":[
                  {"id":"%s","deleted":false,"drugName":"Варфарин"}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        MvcResult pushed = call(MockMvcRequestBuilders.post(SYNC_PUSH_PATH)
                .contentType(MediaType.APPLICATION_JSON).content(syncBody), token);
        validate(METHOD_POST, SYNC_PUSH_PATH, null, token, syncBody, pushed);

        MvcResult pulled = call(MockMvcRequestBuilders.get(SYNC_PULL_PATH), token);
        validate(METHOD_GET, SYNC_PULL_PATH, null, token, null, pulled);
    }

    private MvcResult call(MockHttpServletRequestBuilder builder, String token) throws Exception {
        if (token != null) {
            builder.header("Authorization", BEARER_PREFIX + token);
        }
        return getMockMvc().perform(builder).andReturn();
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
            case METHOD_GET -> SimpleRequest.Builder.get(path);
            case METHOD_POST -> SimpleRequest.Builder.post(path);
            case METHOD_PUT -> SimpleRequest.Builder.put(path);
            case METHOD_DELETE -> SimpleRequest.Builder.delete(path);
            default -> throw new IllegalArgumentException("Unsupported method " + method);
        };
        if (query != null) {
            query.forEach(request::withQueryParam);
        }
        if (token != null) {
            request.withAuthorization(BEARER_PREFIX + token);
        }
        if (requestBody != null) {
            request.withContentType(JSON_CONTENT_TYPE).withBody(requestBody);
        }

        SimpleResponse.Builder response = SimpleResponse.Builder.status(result.getResponse().getStatus());
        String responseBody = result.getResponse().getContentAsString();
        if (!responseBody.isBlank()) {
            response.withContentType(JSON_CONTENT_TYPE).withBody(responseBody);
        }

        ValidationReport report = validator.validate(request.build(), response.build());
        List<String> errors = report.getMessages().stream()
                .filter(message -> message.getLevel() == ValidationReport.Level.ERROR)
                .map(message -> message.getKey() + ": " + message.getMessage())
                .toList();
        Assertions.assertThat(errors).as("Contract violations for %s %s", method, path).isEmpty();
    }
}
