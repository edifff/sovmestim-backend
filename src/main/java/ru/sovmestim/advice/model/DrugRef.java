package ru.sovmestim.advice.model;

import java.util.List;
import java.util.UUID;

/**
 * The drug being checked.
 */
public record DrugRef(UUID medicineId, String name, List<SubstanceRef> substances) {}
