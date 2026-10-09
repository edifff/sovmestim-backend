package ru.sovmestim.catalog;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the catalog endpoints.
 */
class CatalogIT extends PostgresIntegrationTest {

    private static final String RESOLVE_PATH = "/v1/catalog/resolve";
    private static final String SUBSTANCES_PATH = "/v1/catalog/substances";
    private static final String MEDICINES_PATH = "/v1/catalog/medicines";
    private static final String QUERY_PARAM = "query";
    private static final String LIMIT_PARAM = "limit";
    private static final String NAME_PARAM = "name";
    private static final String NAME_PATH = "$.name";
    private static final String FIRST_NAME_PATH = "$[0].name";
    private static final String FIRST_SUBSTANCE_NAME_PATH = "$[0].substances[0].name";
    private static final String MEDICINE_ID_PATH = "$.medicineId";
    private static final String SUBSTANCE_NAME_JSON_PATH = "$.substances[0].name";
    private static final String CODE_PATH = "$.code";
    private static final String SLASH = "/";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String IBUPROFEN = "ибупрофен";
    private static final String WARFARIN = "варфарин";
    private static final String ASPIRIN_CARDIO = "Аспирин Кардио";
    private static final String ACETYLSALICYLIC_ACID = "ацетилсалициловая кислота";

    @Test
    void searchesSubstancesAfterNormalization() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(SUBSTANCES_PATH).param(QUERY_PARAM, "  Варф "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_NAME_PATH).value(WARFARIN))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].atcCode").value("B01AA"));
    }

    @Test
    void blankSubstanceQueryReturnsEmptyList() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(SUBSTANCES_PATH).param(QUERY_PARAM, "   "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());
    }

    @Test
    void searchRespectsTheRequestedLimit() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(SUBSTANCES_PATH)
                        .param(QUERY_PARAM, "и")
                        .param(LIMIT_PARAM, "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1));
    }

    @Test
    void searchesMedicinesByBrandAndReturnsSubstances() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICINES_PATH).param(QUERY_PARAM, ASPIRIN_CARDIO))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_NAME_PATH).value(ASPIRIN_CARDIO))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].brand").value(ASPIRIN_CARDIO))
                .andExpect(MockMvcResultMatchers.jsonPath(FIRST_SUBSTANCE_NAME_PATH).value(ACETYLSALICYLIC_ACID));
    }

    @Test
    void loadsMedicineByIdWithItsSubstances() throws Exception {
        String medicineId = resolveMedicineId(ASPIRIN_CARDIO);

        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICINES_PATH + SLASH + medicineId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(NAME_PATH).value(ASPIRIN_CARDIO))
                .andExpect(MockMvcResultMatchers.jsonPath(SUBSTANCE_NAME_JSON_PATH).value(ACETYLSALICYLIC_ACID));
    }

    @Test
    void unknownMedicineIdIsNotFound() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(MEDICINES_PATH + SLASH + UUID.randomUUID()))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath(CODE_PATH).value(NOT_FOUND));
    }

    @Test
    void resolvesMedicineByNameToItsSubstances() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(RESOLVE_PATH).param(NAME_PARAM, ASPIRIN_CARDIO))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(SUBSTANCE_NAME_JSON_PATH).value(ACETYLSALICYLIC_ACID))
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICINE_ID_PATH).isNotEmpty());
    }

    @Test
    void resolvesActiveSubstanceWhenNoMedicineMatches() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(RESOLVE_PATH).param(NAME_PARAM, IBUPROFEN))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(SUBSTANCE_NAME_JSON_PATH).value(IBUPROFEN));
    }

    @Test
    void unknownNameResolvesToEmptyResult() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(RESOLVE_PATH).param(NAME_PARAM, "такого-препарата-нет"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.substances").isEmpty());
    }

    private String resolveMedicineId(String name) throws Exception {
        MvcResult result = getMockMvc()
                .perform(MockMvcRequestBuilders.get(RESOLVE_PATH).param(NAME_PARAM, name))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(MEDICINE_ID_PATH).isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), MEDICINE_ID_PATH);
    }
}
