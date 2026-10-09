package ru.sovmestim.sync.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.catalog.domain.Medicine;
import ru.sovmestim.catalog.repository.MedicineRepository;
import ru.sovmestim.catalog.service.CatalogService;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.repository.CourseMedicineRepository;
import ru.sovmestim.sync.dto.MedicationChange;

/**
 * Applies medication course changes from a sync push, resolving the catalog medicine when needed.
 */
@Component
@RequiredArgsConstructor
public class MedicationChangeHandler implements SyncChangeHandler<MedicationChange, CourseMedicine> {

    private static final String TYPE = "medication";

    private final CourseMedicineRepository courseMedicineRepository;
    private final MedicineRepository medicineRepository;
    private final CatalogService catalogService;
    private final SyncReferenceData referenceData;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<CourseMedicine> findOwned(UUID userId, MedicationChange change) {
        return courseMedicineRepository
                .findById(change.id())
                .filter(course -> course.getUser().getId().equals(userId));
    }

    @Override
    public void applyFields(CourseMedicine entity, MedicationChange change) {
        entity.setMedicine(resolveMedicine(change));
        entity.setDosage(change.dosage());
        entity.setFrequency(change.frequency());
        entity.setStartDate(change.startDate());
        entity.setStatus(referenceData.status(change.status()));
    }

    @Override
    public CourseMedicine create(AppUser user, MedicationChange change) {
        return CourseMedicine.builder()
                .id(change.id())
                .user(user)
                .medicine(resolveMedicine(change))
                .dosage(change.dosage())
                .frequency(change.frequency())
                .startDate(change.startDate())
                .status(referenceData.status(change.status()))
                .build();
    }

    @Override
    public void save(CourseMedicine entity) {
        courseMedicineRepository.save(entity);
    }

    @Override
    public boolean profileChange() {
        return false;
    }

    private Medicine resolveMedicine(MedicationChange change) {
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
}
