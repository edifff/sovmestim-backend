package ru.sovmestim.gateway.client;

import java.util.List;
import java.util.UUID;

import ru.sovmestim.gateway.dto.MedicineView;
import ru.sovmestim.gateway.dto.ResolvedDrug;
import ru.sovmestim.gateway.dto.SubstanceView;

/**
 * Access to the catalog service.
 */
public interface CatalogClient {

    /**
     * Searches active substances by name.
     *
     * @param query the name fragment
     * @param limit the maximum number of results
     * @return matching substances
     */
    List<SubstanceView> searchSubstances(String query, int limit);

    /**
     * Searches medicines by name or brand.
     *
     * @param query the name fragment
     * @param limit the maximum number of results
     * @return matching medicines
     */
    List<MedicineView> searchMedicines(String query, int limit);

    /**
     * Loads one medicine.
     *
     * @param id the medicine id
     * @return the medicine view
     */
    MedicineView medicine(UUID id);

    /**
     * Resolves a drug name.
     *
     * @param name the entered name
     * @return the resolution, never {@code null}
     */
    ResolvedDrug resolve(String name);
}
