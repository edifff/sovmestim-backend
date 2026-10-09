package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.util.List;

/**
 * Server → client changes after a cursor, including tombstones so the client can apply deletions,
 * and advice records produced by the server-side auto-check.
 *
 * @param allergies allergies changed since the cursor
 * @param conditions conditions changed since the cursor
 * @param medications medications changed since the cursor
 * @param advice advice records produced after the cursor
 * @param nextCursor cursor to pass to the next pull
 */
public record SyncPullResponse(
        List<AllergyChange> allergies,
        List<ConditionChange> conditions,
        List<MedicationChange> medications,
        List<AdviceDelivery> advice,
        Instant nextCursor) { }
