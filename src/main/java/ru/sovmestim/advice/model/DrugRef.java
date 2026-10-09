package ru.sovmestim.advice.model;

import java.util.List;
import java.util.UUID;

/**
 * The drug being checked.
 *
 * @param medicineId catalog medicine id, may be {@code null} when resolved by name or substances
 * @param name drug name as resolved for the check
 * @param substances active substances of the drug
 */
public record DrugRef(UUID medicineId, String name, List<SubstanceRef> substances) { }
