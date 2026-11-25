package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.List;

import ru.sovmestim.gateway.sync.dto.MedicationChange;

/**
 * One medication delta returned by the intake service.
 *
 * @param medications changed courses, including tombstones
 * @param nextCursor cursor to send on the next pull
 */
public record MedicationDelta(List<MedicationChange> medications, Instant nextCursor) { }
