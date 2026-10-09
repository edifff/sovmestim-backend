package ru.sovmestim.patient;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the patient profile endpoints: personal data, allergy CRUD and condition
 * CRUD, including validation, ownership isolation and authentication.
 */
class ProfileIT extends PostgresIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PROFILE_PATH = "/v1/profile";
    private static final String ALLERGIES_PATH = "/v1/profile/allergies";
    private static final String CONDITIONS_PATH = "/v1/profile/conditions";
    private static final String SLASH = "/";
    private static final String ID_PATH = "$.id";
    private static final String FIRST_ID_PATH = "$[0].id";
    private static final String FIRST_NAME_PATH = "$[0].name";
    private static final String FIRST_SEVERITY_PATH = "$[0].severity";
    private static final String ROOT_PATH = "$";
    private static final String NAME_PATH = "$.name";
    private static final String SEVERITY_PATH = "$.severity";
    private static final String MKB_PATH = "$.mkbCode";
    private static final String CODE_PATH = "$.code";
    private static final String ALLERGY_BODY = """
            {"name":"%s","severity":"%s"}
            """;
    private static final String NAME_ONLY_BODY = """
            {"name":"%s"}
            """;
    private static final String PENICILLIN = "пенициллин";
    private static final String MODERATE = "средняя";
    private static final String SEVERE = "тяжелая";
    private static final String HYPERTENSION = "гипертония";
    private static final String ICD10_HYPERTENSION = "I10";
    private static final String ACTIVE_STATUS = "активна";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @Test
    void returnsAuthenticatedUserProfile() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.get(PROFILE_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(ID_PATH).isNotEmpty());
    }

    @Test
    void allergyLifecycleAddUpdateListDelete() throws Exception {
        String auth = bearer(loginAndGetToken());
        String id = createAllergy(auth, PENICILLIN, MODERATE);

        getMockMvc().perform(MockMvcRequestBuilders.get(ALLERGIES_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_ID_PATH).value(id))
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_NAME_PATH).value(PENICILLIN))
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_SEVERITY_PATH).value(MODERATE));

        getMockMvc().perform(MockMvcRequestBuilders.put(ALLERGIES_PATH + SLASH + id)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALLERGY_BODY.formatted(PENICILLIN, SEVERE)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(SEVERITY_PATH).value(SEVERE));

        getMockMvc().perform(MockMvcRequestBuilders.delete(ALLERGIES_PATH + SLASH + id)
                        .header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        getMockMvc().perform(MockMvcRequestBuilders.get(ALLERGIES_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ROOT_PATH).isEmpty());
    }

    @Test
    void conditionLifecycleAddListDelete() throws Exception {
        String auth = bearer(loginAndGetToken());
        MvcResult created = getMockMvc()
                .perform(MockMvcRequestBuilders.post(CONDITIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","mkbCode":"%s","status":"%s","diagnosisDate":"2020-05-01","note":"note"}
                                """.formatted(HYPERTENSION, ICD10_HYPERTENSION, ACTIVE_STATUS)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath(NAME_PATH).value(HYPERTENSION))
                .andExpect(MockMvcResultMatchers.jsonPath(MKB_PATH).value(ICD10_HYPERTENSION))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), ID_PATH);

        getMockMvc().perform(MockMvcRequestBuilders.get(CONDITIONS_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_ID_PATH).value(id));

        getMockMvc().perform(MockMvcRequestBuilders.delete(CONDITIONS_PATH + SLASH + id)
                        .header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        getMockMvc().perform(MockMvcRequestBuilders.get(CONDITIONS_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(ROOT_PATH).isEmpty());
    }

    @Test
    void allergyWithoutNameFailsValidation() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(ALLERGIES_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"severity":"%s"}
                                """.formatted(MODERATE)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(VALIDATION_ERROR));
    }

    @Test
    void conditionWithoutNameFailsValidation() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(CONDITIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(VALIDATION_ERROR));
    }

    @Test
    void updatingUnknownAllergyIsNotFound() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.put(ALLERGIES_PATH + SLASH + UUID.randomUUID())
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NAME_ONLY_BODY.formatted(PENICILLIN)))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(NOT_FOUND));
    }

    @Test
    void cannotUpdateOrDeleteAnotherUsersAllergy() throws Exception {
        String owner = bearer(loginAndGetToken());
        String stranger = bearer(loginAndGetToken());
        String id = createAllergy(owner, PENICILLIN, MODERATE);

        getMockMvc().perform(MockMvcRequestBuilders.put(ALLERGIES_PATH + SLASH + id)
                        .header(AUTHORIZATION, stranger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NAME_ONLY_BODY.formatted(PENICILLIN)))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        getMockMvc().perform(MockMvcRequestBuilders.delete(ALLERGIES_PATH + SLASH + id)
                        .header(AUTHORIZATION, stranger))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        getMockMvc().perform(MockMvcRequestBuilders.get(ALLERGIES_PATH).header(AUTHORIZATION, owner))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_ID_PATH).value(id));
    }

    @Test
    void cannotDeleteAnotherUsersCondition() throws Exception {
        String owner = bearer(loginAndGetToken());
        String stranger = bearer(loginAndGetToken());
        String id = createCondition(owner);

        getMockMvc().perform(MockMvcRequestBuilders.delete(CONDITIONS_PATH + SLASH + id)
                        .header(AUTHORIZATION, stranger))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(PROFILE_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.get(ALLERGIES_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.get(CONDITIONS_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private String createAllergy(String auth, String name, String severity) throws Exception {
        MvcResult created = getMockMvc()
                .perform(MockMvcRequestBuilders.post(ALLERGIES_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALLERGY_BODY.formatted(name, severity)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), ID_PATH);
    }

    private String createCondition(String auth) throws Exception {
        MvcResult created = getMockMvc()
                .perform(MockMvcRequestBuilders.post(CONDITIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","mkbCode":"%s"}
                                """.formatted(HYPERTENSION, ICD10_HYPERTENSION)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), ID_PATH);
    }

    private static String bearer(String token) {
        return BEARER_PREFIX + token;
    }
}
