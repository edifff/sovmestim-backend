package ru.sovmestim.advice.client;

import java.util.List;
import java.util.UUID;

import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Result of resolving a patient-entered drug name.
 *
 * @param medicineId the matched medicine id, or {@code null} when only a substance matched
 * @param name the resolved name
 * @param substances the substances the name resolved to
 */
public record ResolvedDrug(UUID medicineId, String name, List<SubstanceRef> substances) { }
