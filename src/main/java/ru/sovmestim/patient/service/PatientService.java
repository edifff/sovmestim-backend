package ru.sovmestim.patient.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.patient.domain.Allergy;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDisease;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.domain.Mkb;
import ru.sovmestim.patient.domain.SeverityReaction;
import ru.sovmestim.patient.domain.Status;
import ru.sovmestim.patient.dto.AllergyRequest;
import ru.sovmestim.patient.dto.AllergyView;
import ru.sovmestim.patient.dto.ConditionRequest;
import ru.sovmestim.patient.dto.ConditionView;
import ru.sovmestim.patient.repository.AllergyRepository;
import ru.sovmestim.patient.repository.AllergyUserRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseUserRepository;
import ru.sovmestim.patient.repository.MkbRepository;
import ru.sovmestim.patient.repository.SeverityReactionRepository;
import ru.sovmestim.patient.repository.StatusRepository;

/**
 * Profile management: allergies and chronic diseases. Deletions are soft (tombstones) so they can
 * be synced as explicit removals.
 */
@Service
@RequiredArgsConstructor
public class PatientService {

    /** Prefix of the not-found message for a patient allergy record. */
    private static final String ERROR_ALLERGY_NOT_FOUND = "Allergy not found: ";

    private final AppUserRepository userRepository;
    private final AllergyRepository allergyRepository;
    private final AllergyUserRepository allergyUserRepository;
    private final SeverityReactionRepository severityReactionRepository;
    private final ChronicDiseaseRepository chronicDiseaseRepository;
    private final ChronicDiseaseUserRepository chronicDiseaseUserRepository;
    private final MkbRepository mkbRepository;
    private final StatusRepository statusRepository;

    /**
     * Lists the active allergies of the patient.
     *
     * @param userId the patient whose allergies are listed
     * @return the patient's active allergies
     */
    @Transactional(readOnly = true)
    public List<AllergyView> listAllergies(UUID userId) {
        return allergyUserRepository.findActiveByUserId(userId).stream()
                .map(PatientService::toView)
                .toList();
    }

    /**
     * Adds an allergy to the patient's profile, creating the allergy and severity when new.
     *
     * @param userId the patient the allergy is added to
     * @param request the allergy data to store
     * @return the saved allergy
     */
    @Transactional
    public AllergyView addAllergy(UUID userId, AllergyRequest request) {
        AppUser user = requireUser(userId);
        Allergy allergy = allergyRepository
                .findByNameIgnoreCase(request.name())
                .orElseGet(() -> allergyRepository.save(Allergy.builder().name(request.name()).build()));
        SeverityReaction severity = findOrCreateSeverity(request.severity());
        AllergyUser saved = allergyUserRepository.save(AllergyUser.builder()
                .id(UUID.randomUUID())
                .user(user)
                .allergy(allergy)
                .severityReaction(severity)
                .symptoms(request.symptoms())
                .reason(request.reason())
                .updatedAt(Instant.now())
                .build());
        return toView(saved);
    }

    /**
     * Updates an existing allergy of the patient.
     *
     * @param userId the patient the allergy belongs to
     * @param allergyUserId the id of the allergy record to update
     * @param request the new allergy data
     * @return the updated allergy
     */
    @Transactional
    public AllergyView updateAllergy(UUID userId, UUID allergyUserId, AllergyRequest request) {
        AllergyUser entity = allergyUserRepository
                .findByIdAndUserId(allergyUserId, userId)
                .orElseThrow(() -> new NotFoundException(ERROR_ALLERGY_NOT_FOUND + allergyUserId));
        Allergy allergy = allergyRepository
                .findByNameIgnoreCase(request.name())
                .orElseGet(() -> allergyRepository.save(Allergy.builder().name(request.name()).build()));
        entity.setAllergy(allergy);
        entity.setSeverityReaction(findOrCreateSeverity(request.severity()));
        entity.setSymptoms(request.symptoms());
        entity.setReason(request.reason());
        return toView(allergyUserRepository.save(entity));
    }

    /**
     * Soft-deletes an allergy of the patient so it can be synced as an explicit removal.
     *
     * @param userId the patient the allergy belongs to
     * @param allergyUserId the id of the allergy record to delete
     */
    @Transactional
    public void deleteAllergy(UUID userId, UUID allergyUserId) {
        AllergyUser entity = allergyUserRepository
                .findByIdAndUserId(allergyUserId, userId)
                .orElseThrow(() -> new NotFoundException(ERROR_ALLERGY_NOT_FOUND + allergyUserId));
        entity.setDeleted(true);
        entity.setSynced(false);
        allergyUserRepository.save(entity);
    }

    /**
     * Lists the active chronic conditions of the patient.
     *
     * @param userId the patient whose conditions are listed
     * @return the patient's active chronic conditions
     */
    @Transactional(readOnly = true)
    public List<ConditionView> listConditions(UUID userId) {
        return chronicDiseaseUserRepository.findActiveByUserId(userId).stream()
                .map(PatientService::toView)
                .toList();
    }

    /**
     * Adds a chronic condition to the patient's profile, creating directory entries when new.
     *
     * @param userId the patient the condition is added to
     * @param request the condition data to store
     * @return the saved chronic condition
     */
    @Transactional
    public ConditionView addCondition(UUID userId, ConditionRequest request) {
        AppUser user = requireUser(userId);
        Mkb mkb = findOrCreateMkb(request.mkbCode(), request.name());
        ChronicDisease disease = chronicDiseaseRepository
                .findByNameIgnoreCase(request.name())
                .orElseGet(() -> chronicDiseaseRepository.save(ChronicDisease.builder()
                        .name(request.name())
                        .mkb(mkb)
                        .build()));
        ChronicDiseaseUser saved = chronicDiseaseUserRepository.save(ChronicDiseaseUser.builder()
                .id(UUID.randomUUID())
                .user(user)
                .chronicDisease(disease)
                .status(findOrCreateStatus(request.status()))
                .diagnosisDate(request.diagnosisDate())
                .note(request.note())
                .updatedAt(Instant.now())
                .build());
        return toView(saved);
    }

    /**
     * Soft-deletes a chronic condition of the patient so it can be synced as an explicit removal.
     *
     * @param userId the patient the condition belongs to
     * @param conditionId the id of the condition record to delete
     */
    @Transactional
    public void deleteCondition(UUID userId, UUID conditionId) {
        ChronicDiseaseUser entity = chronicDiseaseUserRepository
                .findByIdAndUserId(conditionId, userId)
                .orElseThrow(() -> new NotFoundException("Condition not found: " + conditionId));
        entity.setDeleted(true);
        entity.setSynced(false);
        chronicDiseaseUserRepository.save(entity);
    }

    private AppUser requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
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

    private Status findOrCreateStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return statusRepository
                .findByNameIgnoreCase(status)
                .orElseGet(() -> statusRepository.save(Status.builder().name(status).build()));
    }

    private Mkb findOrCreateMkb(String code, String fallbackName) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return mkbRepository
                .findByCodeIgnoreCase(code)
                .orElseGet(() -> mkbRepository.save(Mkb.builder()
                        .code(code)
                        .name(fallbackName != null ? fallbackName : code)
                        .build()));
    }

    private static AllergyView toView(AllergyUser entity) {
        String severity =
                entity.getSeverityReaction() != null ? entity.getSeverityReaction().getName() : null;
        return new AllergyView(
                entity.getId(),
                entity.getAllergy().getName(),
                entity.getAllergy().getCode(),
                severity,
                entity.getSymptoms(),
                entity.getReason(),
                entity.getUpdatedAt());
    }

    private static ConditionView toView(ChronicDiseaseUser entity) {
        String mkbCode = entity.getChronicDisease().getMkb() != null
                ? entity.getChronicDisease().getMkb().getCode()
                : null;
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new ConditionView(
                entity.getId(),
                entity.getChronicDisease().getName(),
                mkbCode,
                status,
                entity.getDiagnosisDate(),
                entity.getNote(),
                entity.getUpdatedAt());
    }
}
