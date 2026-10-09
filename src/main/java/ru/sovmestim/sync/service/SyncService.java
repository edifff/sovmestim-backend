package ru.sovmestim.sync.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.sovmestim.advice.domain.AdviceRecord;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.repository.AdviceRecordRepository;
import ru.sovmestim.advice.service.AdviceService;
import ru.sovmestim.catalog.domain.Medicine;
import ru.sovmestim.catalog.repository.MedicineRepository;
import ru.sovmestim.catalog.service.CatalogService;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.repository.CourseMedicineRepository;
import ru.sovmestim.intake.service.MedicationService;
import ru.sovmestim.patient.domain.Allergy;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDisease;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.domain.Mkb;
import ru.sovmestim.patient.domain.SeverityReaction;
import ru.sovmestim.patient.domain.Status;
import ru.sovmestim.patient.repository.AllergyRepository;
import ru.sovmestim.patient.repository.AllergyUserRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseUserRepository;
import ru.sovmestim.patient.repository.MkbRepository;
import ru.sovmestim.patient.repository.SeverityReactionRepository;
import ru.sovmestim.patient.repository.StatusRepository;
import ru.sovmestim.sync.domain.SyncRequest;
import ru.sovmestim.sync.dto.SyncPullResponse;
import ru.sovmestim.sync.dto.SyncPushRequest;
import ru.sovmestim.sync.dto.SyncPushResponse;
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
 */
@Service
public class SyncService {

    /** Sync change type of an allergy record. */
    private static final String TYPE_ALLERGY = "allergy";

    /** Sync change type of a chronic condition record. */
    private static final String TYPE_CONDITION = "condition";

    /** Sync change type of a medication record. */
    private static final String TYPE_MEDICATION = "medication";

    /** Rejection message for a change without an id. */
    private static final String ERROR_ID_REQUIRED = "id is required";

    /** Per-record status reported when a change was accepted. */
    private static final String STATUS_APPLIED = "APPLIED";

    /** Per-record status reported when the client change lost to a newer server row. */
    private static final String STATUS_CONFLICT = "CONFLICT";

    /** Per-record status reported when a change was not applied. */
    private static final String STATUS_REJECTED = "REJECTED";

    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);

    private final AppUserRepository userRepository;
    private final AllergyRepository allergyRepository;
    private final AllergyUserRepository allergyUserRepository;
    private final SeverityReactionRepository severityReactionRepository;
    private final ChronicDiseaseRepository chronicDiseaseRepository;
    private final ChronicDiseaseUserRepository chronicDiseaseUserRepository;
    private final MkbRepository mkbRepository;
    private final StatusRepository statusRepository;
    private final CourseMedicineRepository courseMedicineRepository;
    private final MedicineRepository medicineRepository;
    private final CatalogService catalogService;
    private final MedicationService medicationService;
    private final AdviceService adviceService;
    private final AdviceRecordRepository adviceRecordRepository;
    private final SyncRequestRepository syncRequestRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates the service with the repositories and collaborators needed to run sync.
     *
     * @param userRepository repository for patient accounts
     * @param allergyRepository repository for allergy dictionary entries
     * @param allergyUserRepository repository for patient allergies
     * @param severityReactionRepository repository for reaction severity dictionary entries
     * @param chronicDiseaseRepository repository for chronic disease dictionary entries
     * @param chronicDiseaseUserRepository repository for patient conditions
     * @param mkbRepository repository for MKB code dictionary entries
     * @param statusRepository repository for status dictionary entries
     * @param courseMedicineRepository repository for medication courses
     * @param medicineRepository repository for catalog medicines
     * @param catalogService catalog lookups used to resolve drug names
     * @param medicationService access to the patient's active medication courses
     * @param adviceService runs advice checks after a push
     * @param adviceRecordRepository repository for produced advice records
     * @param syncRequestRepository repository for idempotency records
     * @param objectMapper JSON serializer and deserializer for sync payloads
     */
    public SyncService(
            AppUserRepository userRepository,
            AllergyRepository allergyRepository,
            AllergyUserRepository allergyUserRepository,
            SeverityReactionRepository severityReactionRepository,
            ChronicDiseaseRepository chronicDiseaseRepository,
            ChronicDiseaseUserRepository chronicDiseaseUserRepository,
            MkbRepository mkbRepository,
            StatusRepository statusRepository,
            CourseMedicineRepository courseMedicineRepository,
            MedicineRepository medicineRepository,
            CatalogService catalogService,
            MedicationService medicationService,
            AdviceService adviceService,
            AdviceRecordRepository adviceRecordRepository,
            SyncRequestRepository syncRequestRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.allergyRepository = allergyRepository;
        this.allergyUserRepository = allergyUserRepository;
        this.severityReactionRepository = severityReactionRepository;
        this.chronicDiseaseRepository = chronicDiseaseRepository;
        this.chronicDiseaseUserRepository = chronicDiseaseUserRepository;
        this.mkbRepository = mkbRepository;
        this.statusRepository = statusRepository;
        this.courseMedicineRepository = courseMedicineRepository;
        this.medicineRepository = medicineRepository;
        this.catalogService = catalogService;
        this.medicationService = medicationService;
        this.adviceService = adviceService;
        this.adviceRecordRepository = adviceRecordRepository;
        this.syncRequestRepository = syncRequestRepository;
        this.objectMapper = objectMapper;
    }

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

        List<SyncPushResponse.SyncRecordResult> results = new ArrayList<>();
        boolean profileChanged = false;
        Set<UUID> courseIdsToCheck = new LinkedHashSet<>();

        for (SyncPushRequest.AllergyChange change : request.allergiesOrEmpty()) {
            Applied applied = applyAllergy(user, change);
            results.add(applied.result());
            profileChanged |= applied.profileChanged();
        }
        for (SyncPushRequest.ConditionChange change : request.conditionsOrEmpty()) {
            Applied applied = applyCondition(user, change);
            results.add(applied.result());
            profileChanged |= applied.profileChanged();
        }
        for (SyncPushRequest.MedicationChange change : request.medicationsOrEmpty()) {
            Applied applied = applyMedication(user, change);
            results.add(applied.result());
            if (applied.courseIdForCheck() != null) {
                courseIdsToCheck.add(applied.courseIdForCheck());
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
                countByStatus(results, STATUS_CONFLICT),
                countByStatus(results, STATUS_REJECTED),
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

    // ------------------------------------------------------------------ allergies

    private Applied applyAllergy(AppUser user, SyncPushRequest.AllergyChange change) {
        if (change.id() == null) {
            return rejected(TYPE_ALLERGY, null, ERROR_ID_REQUIRED);
        }
        Optional<AllergyUser> existing = allergyUserRepository.findByIdAndUserId(change.id(), user.getId());
        if (existing.isPresent()) {
            AllergyUser entity = existing.get();
            if (change.deleted()) {
                entity.setDeleted(true);
                entity.setSynced(true);
                allergyUserRepository.save(entity);
                return appliedProfile(TYPE_ALLERGY, entity.getId());
            }
            if (isServerNewer(entity.getUpdatedAt(), change.updatedAt())) {
                return conflict(TYPE_ALLERGY, entity.getId());
            }
            entity.setAllergy(findOrCreateAllergy(change.name()));
            entity.setSeverityReaction(findOrCreateSeverity(change.severity()));
            entity.setSymptoms(change.symptoms());
            entity.setReason(change.reason());
            entity.setDeleted(false);
            entity.setSynced(true);
            allergyUserRepository.save(entity);
            return appliedProfile(TYPE_ALLERGY, entity.getId());
        }
        if (change.deleted()) {
            return appliedNoop(TYPE_ALLERGY, change.id());
        }
        AllergyUser created = AllergyUser.builder()
                .id(change.id())
                .user(user)
                .allergy(findOrCreateAllergy(change.name()))
                .severityReaction(findOrCreateSeverity(change.severity()))
                .symptoms(change.symptoms())
                .reason(change.reason())
                .updatedAt(Instant.now())
                .build();
        allergyUserRepository.save(created);
        return appliedProfile(TYPE_ALLERGY, change.id());
    }

    // ------------------------------------------------------------------ conditions

    private Applied applyCondition(AppUser user, SyncPushRequest.ConditionChange change) {
        if (change.id() == null) {
            return rejected(TYPE_CONDITION, null, ERROR_ID_REQUIRED);
        }
        Optional<ChronicDiseaseUser> existing =
                chronicDiseaseUserRepository.findByIdAndUserId(change.id(), user.getId());
        if (existing.isPresent()) {
            ChronicDiseaseUser entity = existing.get();
            if (change.deleted()) {
                entity.setDeleted(true);
                entity.setSynced(true);
                chronicDiseaseUserRepository.save(entity);
                return appliedProfile(TYPE_CONDITION, entity.getId());
            }
            if (isServerNewer(entity.getUpdatedAt(), change.updatedAt())) {
                return conflict(TYPE_CONDITION, entity.getId());
            }
            entity.setChronicDisease(findOrCreateDisease(change.name(), change.mkbCode()));
            entity.setStatus(findOrCreateStatus(change.status()));
            entity.setDiagnosisDate(change.diagnosisDate());
            entity.setNote(change.note());
            entity.setDeleted(false);
            entity.setSynced(true);
            chronicDiseaseUserRepository.save(entity);
            return appliedProfile(TYPE_CONDITION, entity.getId());
        }
        if (change.deleted()) {
            return appliedNoop(TYPE_CONDITION, change.id());
        }
        ChronicDiseaseUser created = ChronicDiseaseUser.builder()
                .id(change.id())
                .user(user)
                .chronicDisease(findOrCreateDisease(change.name(), change.mkbCode()))
                .status(findOrCreateStatus(change.status()))
                .diagnosisDate(change.diagnosisDate())
                .note(change.note())
                .updatedAt(Instant.now())
                .build();
        chronicDiseaseUserRepository.save(created);
        return appliedProfile(TYPE_CONDITION, change.id());
    }

    // ------------------------------------------------------------------ medications

    private Applied applyMedication(AppUser user, SyncPushRequest.MedicationChange change) {
        if (change.id() == null) {
            return rejected(TYPE_MEDICATION, null, ERROR_ID_REQUIRED);
        }
        Optional<CourseMedicine> existing = courseMedicineRepository.findById(change.id())
                .filter(course -> course.getUser().getId().equals(user.getId()));
        if (existing.isPresent()) {
            CourseMedicine entity = existing.get();
            if (change.deleted()) {
                entity.setDeleted(true);
                entity.setSynced(true);
                courseMedicineRepository.save(entity);
                return appliedNoop(TYPE_MEDICATION, entity.getId());
            }
            if (isServerNewer(entity.getUpdatedAt(), change.updatedAt())) {
                return conflict(TYPE_MEDICATION, entity.getId());
            }
            Medicine medicine = resolveMedicine(change);
            entity.setMedicine(medicine);
            entity.setDosage(change.dosage());
            entity.setFrequency(change.frequency());
            entity.setStartDate(change.startDate());
            entity.setStatus(findOrCreateStatus(change.status()));
            entity.setDeleted(false);
            entity.setSynced(true);
            courseMedicineRepository.save(entity);
            return appliedMedication(entity.getId());
        }
        if (change.deleted()) {
            return appliedNoop(TYPE_MEDICATION, change.id());
        }
        Medicine medicine = resolveMedicine(change);
        CourseMedicine created = CourseMedicine.builder()
                .id(change.id())
                .user(user)
                .medicine(medicine)
                .dosage(change.dosage())
                .frequency(change.frequency())
                .startDate(change.startDate())
                .status(findOrCreateStatus(change.status()))
                .updatedAt(Instant.now())
                .build();
        courseMedicineRepository.save(created);
        return appliedMedication(change.id());
    }

    private Medicine resolveMedicine(SyncPushRequest.MedicationChange change) {
        if (change.medicineId() != null) {
            return medicineRepository
                    .findById(change.medicineId())
                    .orElseThrow(() -> new NotFoundException("Medicine not found: " + change.medicineId()));
        }
        if (change.drugName() == null || change.drugName().isBlank()) {
            throw new BadRequestException("Medication change needs medicineId or drugName");
        }
        return catalogService
                .resolveByName(change.drugName())
                .flatMap(resolved -> resolved.medicineId() != null
                        ? medicineRepository.findById(resolved.medicineId())
                        : Optional.<Medicine>empty())
                .orElseGet(() -> medicineRepository.save(
                        Medicine.builder().name(change.drugName()).build()));
    }

    // ------------------------------------------------------------------ find-or-create

    private Allergy findOrCreateAllergy(String name) {
        return allergyRepository
                .findByNameIgnoreCase(name)
                .orElseGet(() -> allergyRepository.save(Allergy.builder().name(name).build()));
    }

    private SeverityReaction findOrCreateSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return null;
        }
        return severityReactionRepository
                .findByNameIgnoreCase(severity)
                .orElseGet(() -> severityReactionRepository.save(
                        SeverityReaction.builder().name(severity).build()));
    }

    private ChronicDisease findOrCreateDisease(String name, String mkbCode) {
        return chronicDiseaseRepository.findByNameIgnoreCase(name).orElseGet(() -> chronicDiseaseRepository.save(
                ChronicDisease.builder().name(name).mkb(findOrCreateMkb(mkbCode, name)).build()));
    }

    private Mkb findOrCreateMkb(String code, String fallbackName) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return mkbRepository
                .findByCodeIgnoreCase(code)
                .orElseGet(() -> mkbRepository.save(Mkb.builder().code(code).name(fallbackName).build()));
    }

    private Status findOrCreateStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return statusRepository
                .findByNameIgnoreCase(status)
                .orElseGet(() -> statusRepository.save(Status.builder().name(status).build()));
    }

    // ------------------------------------------------------------------ helpers

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
                    .createdAt(Instant.now())
                    .build());
        }
    }

    private static Applied appliedProfile(String type, UUID id) {
        return new Applied(result(type, id, STATUS_APPLIED, null), true, null);
    }

    private static Applied appliedMedication(UUID courseId) {
        return new Applied(result(TYPE_MEDICATION, courseId, STATUS_APPLIED, null), false, courseId);
    }

    private static Applied appliedNoop(String type, UUID id) {
        return new Applied(result(type, id, STATUS_APPLIED, "no-op"), false, null);
    }

    private static Applied conflict(String type, UUID id) {
        return new Applied(result(type, id, STATUS_CONFLICT, "server row is newer"), false, null);
    }

    private static Applied rejected(String type, UUID id, String message) {
        return new Applied(result(type, id, STATUS_REJECTED, message), false, null);
    }

    private static long countByStatus(List<SyncPushResponse.SyncRecordResult> results, String status) {
        return results.stream().filter(record -> status.equals(record.status())).count();
    }

    private static SyncPushResponse.SyncRecordResult result(String type, UUID id, String status, String message) {
        return new SyncPushResponse.SyncRecordResult(type, id, status, message);
    }

    private static boolean isServerNewer(Instant server, Instant client) {
        return client != null && server != null && server.isAfter(client);
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

    private static SyncPushRequest.AllergyChange toChange(AllergyUser entity) {
        String severity =
                entity.getSeverityReaction() != null ? entity.getSeverityReaction().getName() : null;
        return new SyncPushRequest.AllergyChange(
                entity.getId(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getAllergy().getName(),
                severity,
                entity.getSymptoms(),
                entity.getReason());
    }

    private static SyncPushRequest.ConditionChange toChange(ChronicDiseaseUser entity) {
        String mkbCode = entity.getChronicDisease().getMkb() != null
                ? entity.getChronicDisease().getMkb().getCode()
                : null;
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new SyncPushRequest.ConditionChange(
                entity.getId(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getChronicDisease().getName(),
                mkbCode,
                status,
                entity.getDiagnosisDate(),
                entity.getNote());
    }

    private static SyncPushRequest.MedicationChange toMedicationChange(CourseMedicine entity) {
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new SyncPushRequest.MedicationChange(
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

    private SyncPullResponse.AdviceDelivery toDelivery(AdviceRecord record) {
        AdviceResult result = read(record.getResultJson(), AdviceResult.class);
        if (result == null) {
            return null;
        }
        return new SyncPullResponse.AdviceDelivery(
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

    /**
     * Outcome of applying a single sync change.
     *
     * @param result result reported to the client for this record
     * @param profileChanged whether the change altered the patient profile
     * @param courseIdForCheck course to recheck with advice, or {@code null}
     */
    private record Applied(SyncPushResponse.SyncRecordResult result, boolean profileChanged, UUID courseIdForCheck) { }
}
