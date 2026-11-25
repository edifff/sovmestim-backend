package ru.sovmestim.intake.service;

import java.time.Instant;
import java.util.List;

import ru.sovmestim.intake.domain.CourseMedicine;

/**
 * One medication delta returned to the gateway for a sync pull.
 *
 * @param medications changed courses, including tombstones
 * @param nextCursor cursor to send on the next pull
 */
public record MedicationDelta(List<CourseMedicine> medications, Instant nextCursor) { }
