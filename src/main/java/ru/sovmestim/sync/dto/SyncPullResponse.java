package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.util.List;

import ru.sovmestim.advice.model.AdviceResult;

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
        List<SyncPushRequest.AllergyChange> allergies,
        List<SyncPushRequest.ConditionChange> conditions,
        List<SyncPushRequest.MedicationChange> medications,
        List<AdviceDelivery> advice,
        Instant nextCursor) {

    /**
     * A check result produced by the server (roadmap S4/S5). {@code result} carries the danger level,
     * findings, sources and rule/catalog versions.
     *
     * @param adviceId id of the advice record
     * @param drugName drug the advice was produced for
     * @param createdAt time the advice was produced
     * @param result check result with danger level, findings and sources
     */
    public record AdviceDelivery(String adviceId, String drugName, Instant createdAt, AdviceResult result) { }
}
