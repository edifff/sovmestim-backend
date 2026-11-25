package ru.sovmestim.gateway.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.gateway.client.CatalogClient;
import ru.sovmestim.gateway.dto.MedicineView;
import ru.sovmestim.gateway.dto.ResolvedDrug;
import ru.sovmestim.gateway.dto.SubstanceView;

/**
 * Public read-only catalog: name autocomplete and drug-to-substance resolution.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/catalog")
public class CatalogController {

    private final CatalogClient catalogClient;

    /**
     * Searches active substances by name.
     *
     * @param query the name fragment to search for
     * @param limit the maximum number of results
     * @return matching substances
     */
    @GetMapping("/substances")
    public List<SubstanceView> substances(
            @RequestParam("query") String query, @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return catalogClient.searchSubstances(query, limit);
    }

    /**
     * Searches medicines by name or trade mark name.
     *
     * @param query the name fragment to search for
     * @param limit the maximum number of results
     * @return matching medicines
     */
    @GetMapping("/medicines")
    public List<MedicineView> medicines(
            @RequestParam("query") String query, @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return catalogClient.searchMedicines(query, limit);
    }

    /**
     * Loads one medicine with all its substances.
     *
     * @param id the medicine identifier
     * @return the medicine view
     */
    @GetMapping("/medicines/{id}")
    public MedicineView medicine(@PathVariable UUID id) {
        return catalogClient.medicine(id);
    }

    /**
     * Resolves a drug name to a medicine or an active substance.
     *
     * @param name the drug name as entered by the patient
     * @return the resolution result, or an empty resolution when nothing matches
     */
    @GetMapping("/resolve")
    public ResolvedDrug resolve(@RequestParam("name") String name) {
        return catalogClient.resolve(name);
    }
}
