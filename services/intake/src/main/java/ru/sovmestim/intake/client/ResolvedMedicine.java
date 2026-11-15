package ru.sovmestim.intake.client;

import java.util.UUID;

/**
 * Minimal view of a catalog medicine needed by the intake service.
 *
 * @param id the catalog medicine id, or {@code null} for a local draft
 * @param name the medicine name as entered or resolved
 * @param brand the trade mark brand, may be {@code null}
 */
public record ResolvedMedicine(UUID id, String name, String brand) { }
