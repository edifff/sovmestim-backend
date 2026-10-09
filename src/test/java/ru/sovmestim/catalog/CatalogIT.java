package ru.sovmestim.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.sovmestim.support.PostgresIntegrationTest;

/**
 * Integration tests for the catalog endpoints.
 */
class CatalogIT extends PostgresIntegrationTest {

    private static final String RESOLVE_PATH = "/v1/catalog/resolve";
    private static final String NAME_PARAM = "name";
    private static final String SUBSTANCE_NAME_JSON_PATH = "$.substances[0].name";
    private static final String IBUPROFEN = "ибупрофен";

    @Test
    void searchesSubstancesAfterNormalization() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get("/v1/catalog/substances").param("query", "  Варф "))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("варфарин"));
    }

    @Test
    void resolvesMedicineByNameToItsSubstances() throws Exception {
        getMockMvc().perform(MockMvcRequestBuilders.get(RESOLVE_PATH).param(NAME_PARAM, "Аспирин Кардио"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(SUBSTANCE_NAME_JSON_PATH).value("ацетилсалициловая кислота"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.medicineId").isNotEmpty());
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
}
