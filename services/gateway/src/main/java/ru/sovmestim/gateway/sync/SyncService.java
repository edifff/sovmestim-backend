package ru.sovmestim.gateway.sync;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.gateway.client.AdviceClient;
import ru.sovmestim.gateway.client.AdviceDelta;
import ru.sovmestim.gateway.client.IdentityClient;
import ru.sovmestim.gateway.client.IntakeClient;
import ru.sovmestim.gateway.client.MedicationDelta;
import ru.sovmestim.gateway.client.PatientClient;
import ru.sovmestim.gateway.client.PatientDelta;
import ru.sovmestim.gateway.client.UpsertResult;
import ru.sovmestim.gateway.dto.AllergyRequest;
import ru.sovmestim.gateway.dto.ConditionRequest;
import ru.sovmestim.gateway.sync.domain.SyncRequest;
import ru.sovmestim.gateway.sync.dto.AllergyChange;
import ru.sovmestim.gateway.sync.dto.ConditionChange;
import ru.sovmestim.gateway.sync.dto.MedicationChange;
import ru.sovmestim.gateway.sync.dto.SyncPullResponse;
import ru.sovmestim.gateway.sync.dto.SyncPushRequest;
import ru.sovmestim.gateway.sync.dto.SyncPushResponse;
import ru.sovmestim.gateway.sync.dto.SyncRecordResult;
import ru.sovmestim.gateway.sync.repository.SyncRequestRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Sync orchestration for the profile and current medications, with the server-side advice loop.
 *
 * <p>Push is idempotent by {@code idempotencyKey}; each change is applied by the owning service
 * over gRPC with last-write-wins conflict detection, and the gateway re-runs advice for the affected
 * courses. Pull merges the three service deltas and advances the cursor to the newest change.
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);

    private static final String ALLERGY = "allergy";
    private static final String CONDITION = "condition";
    private static final String MEDICATION = "medication";

    private final IdentityClient identityClient;
    private final PatientClient patientClient;
    private final IntakeClient intakeClient;
    private final AdviceClient adviceClient;
    private final SyncRequestRepository syncRequestRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Applies a client push batch idempotently and rechecks advice for the affected courses.
     *
     * @param userId the patient's user id
     * @param request the batch of changes sent by the client
     * @return per-record results, or the stored response when the idempotency key was already used
     */
    public SyncPushResponse push(UUID userId, SyncPushRequest request) {
        String idempotencyKey = blankToNull(request.idempotencyKey());
        SyncPushResponse cached = cachedResponse(userId, idempotencyKey);
        if (cached != null) {
            LOG.debug("Sync push for user {} replayed from idempotency key {}", userId, idempotencyKey);
            return cached;
        }
        identityClient.user(userId);

        List<SyncRecordResult> results = new ArrayList<>();
        boolean profileChanged = false;
        Set<UUID> courseIdsToCheck = new LinkedHashSet<>();

        for (AllergyChange change : request.allergiesOrEmpty()) {
            ChangeResult result = applyAllergy(userId, change);
            results.add(toRecord(ALLERGY, result));
            profileChanged |= applied(result);
        }
        for (ConditionChange change : request.conditionsOrEmpty()) {
            ChangeResult result = applyCondition(userId, change);
            results.add(toRecord(CONDITION, result));
            profileChanged |= applied(result);
        }
        for (MedicationChange change : request.medicationsOrEmpty()) {
            UpsertResult result = applyMedication(userId, change);
            results.add(toRecord(MEDICATION, result.result()));
            if (result.courseIdForCheck() != null) {
                courseIdsToCheck.add(result.courseIdForCheck());
            }
        }

        if (profileChanged) {
            adviceClient.recheckAll(userId);
        } else if (!courseIdsToCheck.isEmpty()) {
            adviceClient.recheckCourses(userId, courseIdsToCheck);
        }

        SyncPushResponse response = new SyncPushResponse(results);
        storeResponse(userId, idempotencyKey, response);
        LOG.info(
                "Sync push for user {}: {} record(s), {} conflict(s), {} rejected, advice rechecks={}",
                userId,
                results.size(),
                countByStatus(results, ChangeResult.CONFLICT),
                countByStatus(results, ChangeResult.REJECTED),
                courseIdsToCheck.size());
        return response;
    }

    /**
     * Returns everything changed after the cursor, including tombstones and the next cursor.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor, or {@code null} to start from the epoch
     * @return changed allergies, conditions, medications, advice records and the next cursor
     */
    public SyncPullResponse pull(UUID userId, Instant cursor) {
        PatientDelta profile = patientClient.changesSince(userId, cursor);
        MedicationDelta medications = intakeClient.changesSince(userId, cursor);
        AdviceDelta advice = adviceClient.adviceSince(userId, cursor);

        Instant next = later(cursor, profile.nextCursor());
        next = later(next, medications.nextCursor());
        next = later(next, advice.nextCursor());

        LOG.debug(
                "Sync pull for user {}: {} allergy, {} condition, {} medication change(s), {} advice record(s)",
                userId,
                profile.allergies().size(),
                profile.conditions().size(),
                medications.medications().size(),
                advice.deliveries().size());
        return new SyncPullResponse(
                profile.allergies(),
                profile.conditions(),
                medications.medications(),
                advice.deliveries(),
                next != null ? next : Instant.EPOCH);
    }

    private ChangeResult applyAllergy(UUID userId, AllergyChange change) {
        if (change.deleted()) {
            return patientClient.deleteAllergy(userId, change.id(), change.updatedAt());
        }
        AllergyRequest request =
                new AllergyRequest(change.name(), change.severity(), change.symptoms(), change.reason());
        return patientClient.upsertAllergy(userId, change.id(), change.updatedAt(), request);
    }

    private ChangeResult applyCondition(UUID userId, ConditionChange change) {
        if (change.deleted()) {
            return patientClient.deleteCondition(userId, change.id(), change.updatedAt());
        }
        ConditionRequest request = new ConditionRequest(
                change.name(),
                change.mkbCode(),
                change.status(),
                change.diagnosisDate(),
                change.note());
        return patientClient.upsertCondition(userId, change.id(), change.updatedAt(), request);
    }

    private UpsertResult applyMedication(UUID userId, MedicationChange change) {
        if (change.deleted()) {
            return new UpsertResult(
                    intakeClient.deleteMedication(userId, change.id(), change.updatedAt()), null);
        }
        return intakeClient.upsertMedication(
                userId,
                change.id(),
                change.updatedAt(),
                change.medicineId(),
                change.drugName(),
                change.dosage(),
                change.frequency(),
                change.startDate(),
                change.status());
    }

    private SyncPushResponse cachedResponse(UUID userId, String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        return syncRequestRepository
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(SyncRequest::getResponseJson)
                .map(json -> read(json, SyncPushResponse.class))
                .orElse(null);
    }

    private void storeResponse(UUID userId, String idempotencyKey, SyncPushResponse response) {
        if (idempotencyKey != null) {
            syncRequestRepository.save(SyncRequest.builder()
                    .userId(userId)
                    .idempotencyKey(idempotencyKey)
                    .responseJson(write(response))
                    .createdAt(clock.instant())
                    .build());
        }
    }

    private static SyncRecordResult toRecord(String entityType, ChangeResult result) {
        return new SyncRecordResult(entityType, result.id(), result.status(), result.message());
    }

    private static boolean applied(ChangeResult result) {
        return ChangeResult.APPLIED.equals(result.status());
    }

    private static long countByStatus(List<SyncRecordResult> results, String status) {
        return results.stream().filter(record -> status.equals(record.status())).count();
    }

    private static Instant later(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return candidate.isAfter(current) ? candidate : current;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            LOG.warn("Cannot serialize {} for the sync response", value.getClass().getSimpleName(), ex);
            return null;
        }
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            LOG.warn("Cannot deserialize stored sync payload into {}", type.getSimpleName(), ex);
            return null;
        }
    }
}
