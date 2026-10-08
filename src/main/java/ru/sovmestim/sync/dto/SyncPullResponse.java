package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.util.List;
import ru.sovmestim.advice.model.AdviceResult;

/**
 * Server → client changes after a cursor, including tombstones so the client can apply deletions,
 * and advice records produced by the server-side auto-check.
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
     */
    public record AdviceDelivery(String adviceId, String drugName, Instant createdAt, AdviceResult result) {}
}
