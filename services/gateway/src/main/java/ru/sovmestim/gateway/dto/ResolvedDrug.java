package ru.sovmestim.gateway.dto;

import java.util.List;
import java.util.UUID;


/**
 * Result of resolving a patient-entered drug name to the catalog.
 *
 * @param medicineId the matched medicine identifier, or {@code null} when only a substance matched
 * @param name the resolved name
 * @param substances the substances the entered name resolved to
 */
public record ResolvedDrug(UUID medicineId, String name, List<SubstanceRef> substances) {

    /**
     * Tells whether the entered name resolved to at least one substance.
     *
     * @return {@code true} when at least one substance was resolved
     */
    public boolean resolved() {
        return !substances.isEmpty();
    }
}
