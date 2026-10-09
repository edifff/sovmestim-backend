package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single chronic condition change identified by a client-generated id.
 *
 * @param id client-generated UUIDv7; required so sync is idempotent
 * @param updatedAt client modification time used for conflict detection
 * @param deleted whether the condition is deleted on the client
 * @param name condition name
 * @param mkbCode MKB code of the condition
 * @param status status name
 * @param diagnosisDate date of diagnosis
 * @param note free-form note
 */
public record ConditionChange(
        UUID id,
        Instant updatedAt,
        boolean deleted,
        String name,
        String mkbCode,
        String status,
        LocalDate diagnosisDate,
        String note)
        implements SyncChange { }
