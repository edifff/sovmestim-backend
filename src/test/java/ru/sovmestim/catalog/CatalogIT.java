package ru.sovmestim.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import ru.sovmestim.support.PostgresIntegrationTest;

class CatalogIT extends PostgresIntegrationTest {

    @Test
    void searchesSubstancesAfterNormalization() throws Exception {
        mockMvc.perform(get("/v1/catalog/substances").param("query", "  Варф "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("варфарин"));
    }

    @Test
    void resolvesMedicineByNameToItsSubstances() throws Exception {
        mockMvc.perform(get("/v1/catalog/resolve").param("name", "Аспирин Кардио"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.substances[0].name").value("ацетилсалициловая кислота"))
                .andExpect(jsonPath("$.medicineId").isNotEmpty());
    }

    @Test
    void resolvesActiveSubstanceWhenNoMedicineMatches() throws Exception {
        mockMvc.perform(get("/v1/catalog/resolve").param("name", "ибупрофен"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.substances[0].name").value("ибупрофен"));
    }

    @Test
    void unknownNameResolvesToEmptyResult() throws Exception {
        mockMvc.perform(get("/v1/catalog/resolve").param("name", "такого-препарата-нет"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.substances").isEmpty());
    }
}
