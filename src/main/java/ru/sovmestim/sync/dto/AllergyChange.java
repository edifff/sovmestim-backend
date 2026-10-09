package ru.sovmestim.sync.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A single allergy change identified by a client-generated id.
 *
 * @param id client-generated UUIDv7; required so sync is idempotent
 * @param updatedAt client modification time used for conflict detection
 * @param deleted whether the allergy is deleted on the client
 * @param name allergy name
 * @param severity reaction severity name
 * @param symptoms symptom description
 * @param reason reason for the allergy
 */
public record AllergyChange(
        UUID id, Instant updatedAt, boolean deleted, String name, String severity, String symptoms, String reason)
        implements SyncChange { }
