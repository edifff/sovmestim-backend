package ru.sovmestim.sync.dto;

import java.util.UUID;

/**
 * Outcome of a single submitted change.
 *
 * @param entityType changed entity type, such as {@code allergy}
 * @param id id of the changed record
 * @param status APPLIED | CONFLICT | REJECTED
 * @param message human-readable detail, or {@code null}
 */
public record SyncRecordResult(String entityType, UUID id, String status, String message) { }
