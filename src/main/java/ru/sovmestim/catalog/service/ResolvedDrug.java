package ru.sovmestim.catalog.service;

import java.util.List;
import java.util.UUID;
import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Result of resolving a patient-entered drug name to the catalog.
 */
public record ResolvedDrug(UUID medicineId, String name, List<SubstanceRef> substances) {

    public boolean resolved() {
        return !substances.isEmpty();
    }
}
