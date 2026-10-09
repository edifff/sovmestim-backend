package ru.sovmestim.sync.dto;

import java.util.List;

/**
 * Result of a push batch, with one entry per submitted change.
 *
 * @param results per-record results in submission order
 */
public record SyncPushResponse(List<SyncRecordResult> results) { }
