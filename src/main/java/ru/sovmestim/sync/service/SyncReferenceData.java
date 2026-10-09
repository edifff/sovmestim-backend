package ru.sovmestim.sync.service;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.patient.domain.Allergy;
import ru.sovmestim.patient.domain.ChronicDisease;
import ru.sovmestim.patient.domain.Mkb;
import ru.sovmestim.patient.domain.SeverityReaction;
import ru.sovmestim.patient.domain.Status;
import ru.sovmestim.patient.repository.AllergyRepository;
import ru.sovmestim.patient.repository.ChronicDiseaseRepository;
import ru.sovmestim.patient.repository.MkbRepository;
import ru.sovmestim.patient.repository.SeverityReactionRepository;
import ru.sovmestim.patient.repository.StatusRepository;

/**
 * Resolves the directory entries referenced by incoming sync changes, creating them on first use.
 * Shared by the sync change handlers so normalization and creation stay consistent.
 */
@Component
@RequiredArgsConstructor
public class SyncReferenceData {

    private final AllergyRepository allergyRepository;
    private final SeverityReactionRepository severityReactionRepository;
    private final ChronicDiseaseRepository chronicDiseaseRepository;
    private final MkbRepository mkbRepository;
    private final StatusRepository statusRepository;

    /**
     * Finds or creates an allergy by name.
     *
     * @param name the allergy name
     * @return the stored allergy
     */
    public Allergy allergy(String name) {
        return allergyRepository
                .findByNameIgnoreCase(name)
                .orElseGet(() -> allergyRepository.save(Allergy.builder().name(name).build()));
    }

    /**
     * Finds or creates a reaction severity.
     *
     * @param value the severity name, may be blank
     * @return the stored severity, or {@code null} when the value is blank
     */
    public SeverityReaction severity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return severityReactionRepository
                .findByNameIgnoreCase(value)
                .orElseGet(() -> severityReactionRepository.save(
                        SeverityReaction.builder().name(value).build()));
    }

    /**
     * Finds or creates a chronic disease, creating its MKB code when needed.
     *
     * @param name the disease name
     * @param mkbCode the MKB code, may be blank
     * @return the stored chronic disease
     */
    public ChronicDisease disease(String name, String mkbCode) {
        return chronicDiseaseRepository
                .findByNameIgnoreCase(name)
                .orElseGet(() -> chronicDiseaseRepository.save(
                        ChronicDisease.builder().name(name).mkb(mkb(mkbCode, name)).build()));
    }

    /**
     * Finds or creates a condition status.
     *
     * @param value the status name, may be blank
     * @return the stored status, or {@code null} when the value is blank
     */
    public Status status(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return statusRepository
                .findByNameIgnoreCase(value)
                .orElseGet(() -> statusRepository.save(Status.builder().name(value).build()));
    }

    private Mkb mkb(String code, String fallbackName) {
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
}
