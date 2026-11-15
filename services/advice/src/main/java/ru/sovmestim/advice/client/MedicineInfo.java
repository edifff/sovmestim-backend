package ru.sovmestim.advice.client;

import java.util.List;
import java.util.UUID;

import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Catalog medicine as seen by the advice service.
 *
 * @param id the medicine id
 * @param name the medicine name
 * @param substances the active substances of the medicine
 */
public record MedicineInfo(UUID id, String name, List<SubstanceRef> substances) { }
