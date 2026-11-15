package ru.sovmestim.intake.service;

import java.util.UUID;

/**
 * One substance-level intake rule exposed to the advice service.
 *
 * @param medicineId catalog medicine id, or {@code null}
 * @param activeSubstanceId catalog substance id, or {@code null}
 */
public record TakenSubstanceRef(UUID medicineId, UUID activeSubstanceId) { }
