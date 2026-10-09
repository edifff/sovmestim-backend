package ru.sovmestim.sync.service;

import java.util.UUID;

import ru.sovmestim.sync.dto.SyncRecordResult;

/**
 * Outcome of applying a single sync change.
 *
 * @param result result reported to the client for this record
 * @param profileChanged whether the change altered the patient profile
 * @param courseIdForCheck course to recheck with advice, or {@code null}
 */
public record SyncApplyOutcome(SyncRecordResult result, boolean profileChanged, UUID courseIdForCheck) {

    /** Status of a change that was accepted. */
    public static final String APPLIED = "APPLIED";

    /** Status of a change that lost to a newer server row. */
    public static final String CONFLICT = "CONFLICT";

    /** Status of a change that was not applied. */
    public static final String REJECTED = "REJECTED";

    private static final String CONFLICT_MESSAGE = "server row is newer";
    private static final String NOOP_MESSAGE = "no-op";

    /**
     * Outcome for an applied change that affects the patient profile.
     *
     * @param type the entity type name
     * @param id the record id
     * @return the outcome
     */
    public static SyncApplyOutcome profileApplied(String type, UUID id) {
        return new SyncApplyOutcome(result(type, id, APPLIED, null), true, null);
    }

    /**
     * Outcome for an applied medication change.
     *
     * @param type the entity type name
     * @param id the medication course id to recheck
     * @return the outcome
     */
    public static SyncApplyOutcome medicationApplied(String type, UUID id) {
        return new SyncApplyOutcome(result(type, id, APPLIED, null), false, id);
    }

    /**
     * Outcome for a change that lost the conflict check.
     *
     * @param type the entity type name
     * @param id the record id
     * @return the outcome
     */
    public static SyncApplyOutcome conflict(String type, UUID id) {
        return new SyncApplyOutcome(result(type, id, CONFLICT, CONFLICT_MESSAGE), false, null);
    }

    /**
     * Outcome for a change that could not be applied.
     *
     * @param type the entity type name
     * @param id the record id, may be {@code null}
     * @param message human-readable reason
     * @return the outcome
     */
    public static SyncApplyOutcome rejected(String type, UUID id, String message) {
        return new SyncApplyOutcome(result(type, id, REJECTED, message), false, null);
    }

    /**
     * Outcome for a deletion of a record that does not exist.
     *
     * @param type the entity type name
     * @param id the record id
     * @return the outcome
     */
    public static SyncApplyOutcome noop(String type, UUID id) {
        return new SyncApplyOutcome(result(type, id, APPLIED, NOOP_MESSAGE), false, null);
    }

    private static SyncRecordResult result(String type, UUID id, String status, String message) {
        return new SyncRecordResult(type, id, status, message);
    }
}
