package ru.sovmestim.intake;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the medication course endpoints, covering resolution by catalog id and by
 * free-text name, listing, deletion, validation, ownership isolation and authentication.
 */
class MedicationIT extends PostgresIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MEDICATIONS_PATH = "/v1/medications";
    private static final String CATALOG_MEDICINES_PATH = "/v1/catalog/medicines";
    private static final String QUERY_PARAM = "query";
    private static final String SLASH = "/";
    private static final String ID_PATH = "$.id";
    private static final String FIRST_ID_PATH = "$[0].id";
    private static final String FIRST_MEDICINE_NAME_PATH = "$[0].medicineName";
    private static final String MEDICINE_NAME_PATH = "$.medicineName";
    private static final String CODE_PATH = "$.code";
    private static final String DRUG_NAME_BODY = """
            {"drugName":"%s"}
            """;
    private static final String MEDICINE_ID_BODY = """
            {"medicineId":"%s","dosage":"2.5 мг","frequency":"1 раз в день"}
            """;
    private static final String EMPTY_BODY = "{}";
    private static final String WARFARIN = "Варфарин";
    private static final String UNKNOWN_DRUG = "несуществующее средство xyz";
    private static final String BAD_REQUEST = "BAD_REQUEST";
    private static final String NOT_FOUND = "NOT_FOUND";

    @Test
    void addByDrugNameResolvesCatalogMedicineAndListsIt() throws Exception {
        String auth = bearer(loginAndGetToken());

        addByDrugName(auth, WARFARIN);

        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICATIONS_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_MEDICINE_NAME_PATH).value(WARFARIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].brand").value(WARFARIN));
    }

    @Test
    void addByMedicineIdBindsToCatalogMedicine() throws Exception {
        String auth = bearer(loginAndGetToken());
        String medicineId = resolveMedicineId();

        getMockMvc().perform(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MEDICINE_ID_BODY.formatted(medicineId)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICINE_NAME_PATH).value(WARFARIN));
    }

    @Test
    void unresolvedDrugNameIsStoredAsDraftCourse() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRUG_NAME_BODY.formatted(UNKNOWN_DRUG)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICINE_NAME_PATH).value(UNKNOWN_DRUG));
    }

    @Test
    void deleteRemovesCourseFromList() throws Exception {
        String auth = bearer(loginAndGetToken());
        String id = addByDrugName(auth, WARFARIN);

        getMockMvc().perform(MockMvcRequestBuilders.delete(MEDICATIONS_PATH + SLASH + id)
                        .header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isNoContent());

        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICATIONS_PATH).header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());
    }

    @Test
    void addWithoutMedicineInformationIsBadRequest() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_BODY))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(BAD_REQUEST));
    }

    @Test
    void addWithUnknownMedicineIdIsNotFound() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"medicineId":"%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(NOT_FOUND));
    }

    @Test
    void cannotDeleteAnotherUsersCourse() throws Exception {
        String owner = bearer(loginAndGetToken());
        String stranger = bearer(loginAndGetToken());
        String id = addByDrugName(owner, WARFARIN);

        getMockMvc().perform(MockMvcRequestBuilders.delete(MEDICATIONS_PATH + SLASH + id)
                        .header(AUTHORIZATION, stranger))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICATIONS_PATH).header(AUTHORIZATION, owner))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_ID_PATH).value(id));
    }

    @Test
    void deletingUnknownCourseIsNotFound() throws Exception {
        String auth = bearer(loginAndGetToken());

        getMockMvc().perform(MockMvcRequestBuilders.delete(MEDICATIONS_PATH + SLASH + UUID.randomUUID())
                        .header(AUTHORIZATION, auth))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(NOT_FOUND));
    }

    @Test
    void medicationEndpointsRequireAuthentication() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICATIONS_PATH))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        getMockMvc().perform(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMPTY_BODY))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    private String resolveMedicineId() throws Exception {
        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(CATALOG_MEDICINES_PATH).param(QUERY_PARAM, WARFARIN))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_ID_PATH).isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), FIRST_ID_PATH);
    }

    private String addByDrugName(String auth, String name) throws Exception {
        MvcResult created = getMockMvc()
                .perform(MockMvcRequestBuilders.post(MEDICATIONS_PATH)
                        .header(AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DRUG_NAME_BODY.formatted(name)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), ID_PATH);
    }

    private static String bearer(String token) {
        return BEARER_PREFIX + token;
    }
}
