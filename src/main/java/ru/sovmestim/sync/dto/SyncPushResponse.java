package ru.sovmestim.sync.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result of a push batch, with one entry per submitted change.
 *
 * @param results per-record results in submission order
 */
public record SyncPushResponse(List<SyncRecordResult> results) {

    /**
     * Outcome of a single submitted change.
     *
     * @param entityType changed entity type, such as {@code allergy}
     * @param id id of the changed record
     * @param status APPLIED | CONFLICT | REJECTED
     * @param message human-readable detail, or {@code null}
     */
    public record SyncRecordResult(String entityType, UUID id, String status, String message) { }
}
