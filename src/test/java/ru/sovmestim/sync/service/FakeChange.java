package ru.sovmestim.sync.service;

import java.time.Instant;
import java.util.UUID;

import ru.sovmestim.sync.dto.SyncChange;

/**
 * Minimal change used to drive {@link SyncedChangeApplier} in tests.
 *
 * @param id the record id
 * @param updatedAt the client modification time
 * @param deleted whether the change is a deletion
 */
record FakeChange(UUID id, Instant updatedAt, boolean deleted) implements SyncChange { }
