package ru.sovmestim.intake.service;

import java.util.UUID;

import ru.sovmestim.common.sync.ChangeResult;

/**
 * Result of a medication sync upsert: the change outcome plus the course that advice must recheck.
 *
 * @param result the outcome reported to the client
 * @param courseIdForCheck course to recheck with advice, or {@code null}
 */
public record UpsertResult(ChangeResult result, UUID courseIdForCheck) { }
