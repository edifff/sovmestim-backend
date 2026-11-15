package ru.sovmestim.gateway.client;

import java.util.UUID;

import ru.sovmestim.common.sync.ChangeResult;

/**
 * Result of a medication sync upsert.
 *
 * @param result the write outcome
 * @param courseIdForCheck the course to recheck, or {@code null}
 */
public record UpsertResult(ChangeResult result, UUID courseIdForCheck) { }
