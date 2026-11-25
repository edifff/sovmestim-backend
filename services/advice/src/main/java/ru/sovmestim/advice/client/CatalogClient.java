package ru.sovmestim.advice.client;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Access to the catalog service needed by the advice engine.
 */
public interface CatalogClient {

    /**
     * Loads a medicine with its substances.
     *
     * @param id the medicine id
     * @return the medicine
     */
    MedicineInfo medicine(UUID id);

    /**
     * Resolves a patient-entered drug name.
     *
     * @param name the entered name
     * @return the resolution, or empty when nothing matched
     */
    Optional<ResolvedDrug> resolve(String name);

    /**
     * Loads substances by id.
     *
     * @param ids the substance ids
     * @return the matching substances
     */
    List<SubstanceRef> substancesByIds(List<UUID> ids);

    /**
     * Loads the substances of one medicine.
     *
     * @param medicineId the medicine id
     * @return the substances of the medicine
     */
    List<SubstanceRef> substancesForMedicine(UUID medicineId);

    /**
     * Loads the distinct substances of several medicines.
     *
     * @param medicineIds the medicine ids
     * @return the distinct substances
     */
    List<SubstanceRef> substancesForMedicines(Collection<UUID> medicineIds);
}
