package ru.sovmestim.sync.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.repository.AllergyUserRepository;
import ru.sovmestim.sync.dto.AllergyChange;

/**
 * Applies allergy changes from a sync push.
 */
@Component
@RequiredArgsConstructor
public class AllergyChangeHandler implements SyncChangeHandler<AllergyChange, AllergyUser> {

    private static final String TYPE = "allergy";

    private final AllergyUserRepository allergyUserRepository;
    private final SyncReferenceData referenceData;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<AllergyUser> findOwned(UUID userId, AllergyChange change) {
        return allergyUserRepository.findByIdAndUserId(change.id(), userId);
    }

    @Override
    public void applyFields(AllergyUser entity, AllergyChange change) {
        entity.setAllergy(referenceData.allergy(change.name()));
        entity.setSeverityReaction(referenceData.severity(change.severity()));
        entity.setSymptoms(change.symptoms());
        entity.setReason(change.reason());
    }

    @Override
    public AllergyUser create(AppUser user, AllergyChange change) {
        return AllergyUser.builder()
                .id(change.id())
                .user(user)
                .allergy(referenceData.allergy(change.name()))
                .severityReaction(referenceData.severity(change.severity()))
                .symptoms(change.symptoms())
                .reason(change.reason())
                .build();
    }

    @Override
    public void save(AllergyUser entity) {
        allergyUserRepository.save(entity);
    }

    @Override
    public boolean profileChange() {
        return true;
    }
}
