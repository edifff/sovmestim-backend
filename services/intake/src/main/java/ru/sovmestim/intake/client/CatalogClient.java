package ru.sovmestim.intake.client;

import java.util.Optional;
import java.util.UUID;

/**
 * Access to the catalog service. Kept as an interface so the intake service can be tested without
 * a running catalog and so the transport can change without touching the domain.
 */
public interface CatalogClient {

    /**
     * Loads a catalog medicine by id.
     *
     * @param id the medicine id
     * @return the resolved medicine
     */
    ResolvedMedicine medicine(UUID id);

    /**
     * Resolves a patient-entered drug name to a catalog medicine.
     *
     * @param name the entered name
     * @return the resolved medicine, or empty when the catalog does not know the name
     */
    Optional<ResolvedMedicine> resolve(String name);
}
