package ru.sovmestim.advice.client;

import java.util.UUID;

/**
 * One substance-level intake rule as seen by the advice service.
 *
 * @param medicineId the catalog medicine id, or {@code null}
 * @param activeSubstanceId the catalog substance id, or {@code null}
 */
public record TakenSubstanceRef(UUID medicineId, UUID activeSubstanceId) { }
