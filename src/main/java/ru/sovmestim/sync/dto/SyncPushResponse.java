package ru.sovmestim.sync.dto;

import java.util.List;
import java.util.UUID;

public record SyncPushResponse(List<SyncRecordResult> results) {

    /**
     * @param status APPLIED | CONFLICT | REJECTED
     */
    public record SyncRecordResult(String entityType, UUID id, String status, String message) {}
}
