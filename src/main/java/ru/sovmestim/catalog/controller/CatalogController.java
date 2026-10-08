package ru.sovmestim.catalog.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.sovmestim.catalog.dto.MedicineView;
import ru.sovmestim.catalog.dto.SubstanceView;
import ru.sovmestim.catalog.service.CatalogService;
import ru.sovmestim.catalog.service.ResolvedDrug;

/**
 * Public read-only catalog: name autocomplete and drug-to-substance resolution.
 */
@RestController
@RequestMapping("/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/substances")
    public List<SubstanceView> substances(
            @RequestParam("query") String query, @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return catalogService.searchSubstances(query, limit);
    }

    @GetMapping("/medicines")
    public List<MedicineView> medicines(
            @RequestParam("query") String query, @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return catalogService.searchMedicines(query, limit);
    }

    @GetMapping("/medicines/{id}")
    public MedicineView medicine(@PathVariable UUID id) {
        return catalogService.getMedicine(id);
    }

    @GetMapping("/resolve")
    public ResolvedDrug resolve(@RequestParam("name") String name) {
        return catalogService
                .resolveByName(name)
                .orElseGet(() -> new ResolvedDrug(null, name, List.of()));
    }
}
