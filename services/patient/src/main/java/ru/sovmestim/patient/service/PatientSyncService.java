package ru.sovmestim.patient.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.dto.AllergyRequest;
import ru.sovmestim.patient.dto.ConditionRequest;
import ru.sovmestim.patient.repository.AllergyUserRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseUserRepository;

/**
 * Sync semantics for the patient profile: idempotent upserts, tombstones and last-write-wins
 * conflict detection on server time. The interactive REST flow still uses {@link PatientService};
 * this service is the path taken by {@code /v1/sync/push}.
 */
@Service
@RequiredArgsConstructor
public class PatientSyncService {

    private static final String ID_REQUIRED = "id is required";

    private final AllergyUserRepository allergyUserRepository;
    private final ChronicDiseaseUserRepository conditionRepository;
    private final SyncReferenceData referenceData;

    /**
     * Creates or updates an allergy from a sync change.
     *
     * @param userId the patient's user id
     * @param id the client-generated record id
     * @param clientUpdatedAt the client modification time, may be {@code null}
     * @param request the allergy fields
     * @return the outcome reported to the client
     */
    @Transactional
    public ChangeResult upsertAllergy(UUID userId, UUID id, Instant clientUpdatedAt, AllergyRequest request) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        Optional<AllergyUser> existing = allergyUserRepository.findByIdAndUserId(id, userId);
        return existing
                .map(entity -> updateAllergy(entity, clientUpdatedAt, request))
                .orElseGet(() -> createAllergy(userId, id, request));
    }

    /**
     * Creates or updates a chronic condition from a sync change.
     *
     * @param userId the patient's user id
     * @param id the client-generated record id
     * @param clientUpdatedAt the client modification time, may be {@code null}
     * @param request the condition fields
     * @return the outcome reported to the client
     */
    @Transactional
    public ChangeResult upsertCondition(UUID userId, UUID id, Instant clientUpdatedAt, ConditionRequest request) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        Optional<ChronicDiseaseUser> existing = conditionRepository.findByIdAndUserId(id, userId);
        return existing
                .map(entity -> updateCondition(entity, clientUpdatedAt, request))
                .orElseGet(() -> createCondition(userId, id, request));
    }

    /**
     * Applies an allergy tombstone from a sync change.
     *
     * @param userId the patient's user id
     * @param id the record id
     * @return the outcome reported to the client
     */
    @Transactional
    public ChangeResult deleteAllergy(UUID userId, UUID id) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        Optional<AllergyUser> existing = allergyUserRepository.findByIdAndUserId(id, userId);
        if (existing.isEmpty()) {
            return ChangeResult.applied(id, null);
        }
        AllergyUser entity = existing.get();
        entity.markDeletedSynced();
        AllergyUser saved = allergyUserRepository.save(entity);
        return ChangeResult.applied(id, saved.getUpdatedAt());
    }

    /**
     * Applies a condition tombstone from a sync change.
     *
     * @param userId the patient's user id
     * @param id the record id
     * @return the outcome reported to the client
     */
    @Transactional
    public ChangeResult deleteCondition(UUID userId, UUID id) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        Optional<ChronicDiseaseUser> existing = conditionRepository.findByIdAndUserId(id, userId);
        if (existing.isEmpty()) {
            return ChangeResult.applied(id, null);
        }
        ChronicDiseaseUser entity = existing.get();
        entity.markDeletedSynced();
        ChronicDiseaseUser saved = conditionRepository.save(entity);
        return ChangeResult.applied(id, saved.getUpdatedAt());
    }

    /**
     * Returns profile changes after the cursor, including tombstones.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor, or {@code null} to start from the epoch
     * @return the changed records and the next cursor
     */
    @Transactional(readOnly = true)
    public PatientChanges changesSince(UUID userId, Instant cursor) {
        Instant from = cursor != null ? cursor : Instant.EPOCH;
        List<AllergyUser> allergies = allergyUserRepository.findChangedSince(userId, from);
        List<ChronicDiseaseUser> conditions = conditionRepository.findChangedSince(userId, from);
        Instant next = from;
        for (AllergyUser allergy : allergies) {
            next = later(next, allergy.getUpdatedAt());
        }
        for (ChronicDiseaseUser condition : conditions) {
            next = later(next, condition.getUpdatedAt());
        }
        return new PatientChanges(allergies, conditions, next);
    }

    private ChangeResult updateAllergy(AllergyUser entity, Instant clientUpdatedAt, AllergyRequest request) {
        if (isServerNewer(entity.getUpdatedAt(), clientUpdatedAt)) {
            return ChangeResult.conflict(entity.getId());
        }
        entity.setAllergy(referenceData.allergy(request.name()));
        entity.setSeverityReaction(referenceData.severity(request.severity()));
        entity.setSymptoms(request.symptoms());
        entity.setReason(request.reason());
        entity.setDeleted(false);
        entity.setSynced(true);
        return ChangeResult.applied(entity.getId(), allergyUserRepository.save(entity).getUpdatedAt());
    }

    private ChangeResult createAllergy(UUID userId, UUID id, AllergyRequest request) {
        AllergyUser created = allergyUserRepository.save(AllergyUser.builder()
                .id(id)
                .userId(userId)
                .allergy(referenceData.allergy(request.name()))
                .severityReaction(referenceData.severity(request.severity()))
                .symptoms(request.symptoms())
                .reason(request.reason())
                .synced(true)
                .build());
        return ChangeResult.applied(created.getId(), created.getUpdatedAt());
    }

    private ChangeResult updateCondition(
            ChronicDiseaseUser entity, Instant clientUpdatedAt, ConditionRequest request) {
        if (isServerNewer(entity.getUpdatedAt(), clientUpdatedAt)) {
            return ChangeResult.conflict(entity.getId());
        }
        entity.setChronicDisease(referenceData.disease(request.name(), request.mkbCode()));
        entity.setStatus(referenceData.status(request.status()));
        entity.setDiagnosisDate(request.diagnosisDate());
        entity.setNote(request.note());
        entity.setDeleted(false);
        entity.setSynced(true);
        return ChangeResult.applied(entity.getId(), conditionRepository.save(entity).getUpdatedAt());
    }

    private ChangeResult createCondition(UUID userId, UUID id, ConditionRequest request) {
        ChronicDiseaseUser created = conditionRepository.save(ChronicDiseaseUser.builder()
                .id(id)
                .userId(userId)
                .chronicDisease(referenceData.disease(request.name(), request.mkbCode()))
                .status(referenceData.status(request.status()))
                .diagnosisDate(request.diagnosisDate())
                .note(request.note())
                .synced(true)
                .build());
        return ChangeResult.applied(created.getId(), created.getUpdatedAt());
    }

    private static boolean isServerNewer(Instant server, Instant client) {
        return client != null && server != null && server.isAfter(client);
    }

    private static Instant later(Instant current, Instant candidate) {
        if (candidate == null) {
            return current;
        }
        return candidate.isAfter(current) ? candidate : current;
    }
}
