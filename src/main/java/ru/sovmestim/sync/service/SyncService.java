package ru.sovmestim.sync.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.domain.AdviceRecord;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.repository.AdviceRecordRepository;
import ru.sovmestim.advice.service.AdviceService;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.repository.CourseMedicineRepository;
import ru.sovmestim.intake.service.MedicationService;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.repository.AllergyUserRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseUserRepository;
import ru.sovmestim.sync.domain.SyncRequest;
import ru.sovmestim.sync.dto.AdviceDelivery;
import ru.sovmestim.sync.dto.AllergyChange;
import ru.sovmestim.sync.dto.ConditionChange;
import ru.sovmestim.sync.dto.MedicationChange;
import ru.sovmestim.sync.dto.SyncPullResponse;
import ru.sovmestim.sync.dto.SyncPushRequest;
import ru.sovmestim.sync.dto.SyncPushResponse;
import ru.sovmestim.sync.dto.SyncRecordResult;
import ru.sovmestim.sync.repository.SyncRequestRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Sync for the patient profile and current medications, with the server-side advice loop.
 *
 * <p>Push is idempotent by {@code idempotencyKey} and last-write-wins per record on server time;
 * a client change older than the stored row is reported as {@code CONFLICT} without overwriting.
 * After applying a batch the server auto-runs advice checks for new/changed medications (and for all
 * active medications when the profile changed); results are delivered as advice in the next pull.
 * Pull returns everything changed after a cursor, including tombstones and the next cursor.
 *
 * <p>The per-record apply flow is shared by {@link SyncedChangeApplier}; the type-specific parts are
 * provided by the {@link SyncChangeHandler} implementations.
 */
@Service
@RequiredArgsConstructor
public class SyncService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);

    private final AppUserRepository userRepository;
    private final AllergyUserRepository allergyUserRepository;
    private final ChronicDiseaseUserRepository chronicDiseaseUserRepository;
    private final CourseMedicineRepository courseMedicineRepository;
    private final AdviceRecordRepository adviceRecordRepository;
    private final MedicationService medicationService;
    private final AdviceService adviceService;
    private final SyncRequestRepository syncRequestRepository;
    private final ObjectMapper objectMapper;
    private final SyncedChangeApplier changeApplier;
    private final AllergyChangeHandler allergyChangeHandler;
    private final ConditionChangeHandler conditionChangeHandler;
    private final MedicationChangeHandler medicationChangeHandler;
    private final Clock clock;

    /**
     * Applies a client push batch idempotently and rechecks advice for the affected courses.
     *
     * @param userId the patient's user id
     * @param request the batch of changes sent by the client
     * @return per-record results, or the stored response when the idempotency key was already used
     */
    @Transactional
    public SyncPushResponse push(UUID userId, SyncPushRequest request) {
        String idempotencyKey = blankToNull(request.idempotencyKey());
        SyncPushResponse cached = cachedResponse(userId, idempotencyKey);
        if (cached != null) {
            LOG.debug("Sync push for user {} replayed from idempotency key {}", userId, idempotencyKey);
            return cached;
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        List<SyncRecordResult> results = new ArrayList<>();
        boolean profileChanged = false;
        Set<UUID> courseIdsToCheck = new LinkedHashSet<>();

        for (AllergyChange change : request.allergiesOrEmpty()) {
            SyncApplyOutcome outcome = changeApplier.apply(user, change, allergyChangeHandler);
            results.add(outcome.result());
            profileChanged |= outcome.profileChanged();
        }
        for (ConditionChange change : request.conditionsOrEmpty()) {
            SyncApplyOutcome outcome = changeApplier.apply(user, change, conditionChangeHandler);
            results.add(outcome.result());
            profileChanged |= outcome.profileChanged();
        }
        for (MedicationChange change : request.medicationsOrEmpty()) {
            SyncApplyOutcome outcome = changeApplier.apply(user, change, medicationChangeHandler);
            results.add(outcome.result());
            if (outcome.courseIdForCheck() != null) {
                courseIdsToCheck.add(outcome.courseIdForCheck());
            }
        }

        if (profileChanged) {
            courseIdsToCheck.addAll(medicationService.activeCourseIds(userId));
        }
        adviceService.recheckCourses(userId, courseIdsToCheck);

        SyncPushResponse response = new SyncPushResponse(results);
        storeResponse(userId, idempotencyKey, response);
        LOG.info(
                "Sync push for user {}: {} record(s), {} conflict(s), {} rejected, advice rechecks={}",
                userId,
                results.size(),
                countByStatus(results, SyncApplyOutcome.CONFLICT),
                countByStatus(results, SyncApplyOutcome.REJECTED),
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
    @Transactional(readOnly = true)
    public SyncPullResponse pull(UUID userId, Instant cursor) {
        Instant from = cursor != null ? cursor : Instant.EPOCH;
        List<AllergyUser> allergies = allergyUserRepository.findChangedSince(userId, from);
        List<ChronicDiseaseUser> conditions = chronicDiseaseUserRepository.findChangedSince(userId, from);
        List<CourseMedicine> medications = courseMedicineRepository.findChangedSince(userId, from);
        List<AdviceRecord> advice =
                adviceRecordRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, from);

        Instant next = from;
        for (AllergyUser allergy : allergies) {
            next = later(next, allergy.getUpdatedAt());
        }
        for (ChronicDiseaseUser condition : conditions) {
            next = later(next, condition.getUpdatedAt());
        }
        for (CourseMedicine medication : medications) {
            next = later(next, medication.getUpdatedAt());
        }
        for (AdviceRecord record : advice) {
            next = later(next, record.getCreatedAt());
        }

        LOG.debug(
                "Sync pull for user {}: {} allergy, {} condition, {} medication change(s), {} advice record(s)",
                userId,
                allergies.size(),
                conditions.size(),
                medications.size(),
                advice.size());
        return new SyncPullResponse(
                allergies.stream().map(SyncService::toChange).toList(),
                conditions.stream().map(SyncService::toChange).toList(),
                medications.stream().map(SyncService::toMedicationChange).toList(),
                advice.stream().map(this::toDelivery).filter(Objects::nonNull).toList(),
                next);
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

    private static long countByStatus(List<SyncRecordResult> results, String status) {
        return results.stream().filter(record -> status.equals(record.status())).count();
    }

    private static Instant later(Instant a, Instant b) {
        if (b == null) {
            return a;
        }
        return b.isAfter(a) ? b : a;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static AllergyChange toChange(AllergyUser entity) {
        String severity =
                entity.getSeverityReaction() != null ? entity.getSeverityReaction().getName() : null;
        return new AllergyChange(
                entity.getId(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getAllergy().getName(),
                severity,
                entity.getSymptoms(),
                entity.getReason());
    }

    private static ConditionChange toChange(ChronicDiseaseUser entity) {
        String mkbCode = entity.getChronicDisease().getMkb() != null
                ? entity.getChronicDisease().getMkb().getCode()
                : null;
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new ConditionChange(
                entity.getId(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getChronicDisease().getName(),
                mkbCode,
                status,
                entity.getDiagnosisDate(),
                entity.getNote());
    }

    private static MedicationChange toMedicationChange(CourseMedicine entity) {
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new MedicationChange(
                entity.getId(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getMedicine().getId(),
                entity.getMedicine().getName(),
                entity.getDosage(),
                entity.getFrequency(),
                entity.getStartDate(),
                status);
    }

    private AdviceDelivery toDelivery(AdviceRecord record) {
        AdviceResult result = read(record.getResultJson(), AdviceResult.class);
        if (result == null) {
            return null;
        }
        return new AdviceDelivery(
                record.getId().toString(), record.getDrugName(), record.getCreatedAt(), result);
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
