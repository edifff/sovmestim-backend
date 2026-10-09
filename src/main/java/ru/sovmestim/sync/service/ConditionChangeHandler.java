package ru.sovmestim.sync.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;
import ru.sovmestim.patient.repository.ChronicDiseaseUserRepository;
import ru.sovmestim.sync.dto.ConditionChange;

/**
 * Applies chronic condition changes from a sync push.
 */
@Component
@RequiredArgsConstructor
public class ConditionChangeHandler implements SyncChangeHandler<ConditionChange, ChronicDiseaseUser> {

    private static final String TYPE = "condition";

    private final ChronicDiseaseUserRepository chronicDiseaseUserRepository;
    private final SyncReferenceData referenceData;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<ChronicDiseaseUser> findOwned(UUID userId, ConditionChange change) {
        return chronicDiseaseUserRepository.findByIdAndUserId(change.id(), userId);
    }

    @Override
    public void applyFields(ChronicDiseaseUser entity, ConditionChange change) {
        entity.setChronicDisease(referenceData.disease(change.name(), change.mkbCode()));
        entity.setStatus(referenceData.status(change.status()));
        entity.setDiagnosisDate(change.diagnosisDate());
        entity.setNote(change.note());
    }

    @Override
    public ChronicDiseaseUser create(AppUser user, ConditionChange change) {
        return ChronicDiseaseUser.builder()
                .id(change.id())
                .user(user)
                .chronicDisease(referenceData.disease(change.name(), change.mkbCode()))
                .status(referenceData.status(change.status()))
                .diagnosisDate(change.diagnosisDate())
                .note(change.note())
                .build();
    }

    @Override
    public void save(ChronicDiseaseUser entity) {
        chronicDiseaseUserRepository.save(entity);
    }

    @Override
    public boolean profileChange() {
        return true;
    }
}
