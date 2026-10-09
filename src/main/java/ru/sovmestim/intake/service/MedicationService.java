package ru.sovmestim.intake.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.catalog.domain.Medicine;
import ru.sovmestim.catalog.repository.MedicineRepository;
import ru.sovmestim.catalog.service.CatalogService;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.identity.domain.AppUser;
import ru.sovmestim.identity.repository.AppUserRepository;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.dto.CourseMedicineRequest;
import ru.sovmestim.intake.dto.CourseMedicineView;
import ru.sovmestim.intake.repository.CourseMedicineRepository;

/**
 * The patient's current drugs. Entered names that are not in the catalog are stored as-is so they
 * can later be normalized and checked on sync (status {@code DRAFT_UNVERIFIED} on the client).
 */
@Service
@RequiredArgsConstructor
public class MedicationService {

    private static final Logger LOG = LoggerFactory.getLogger(MedicationService.class);

    private final AppUserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final CourseMedicineRepository courseMedicineRepository;
    private final CatalogService catalogService;

    /**
     * Lists the patient's active medication courses.
     *
     * @param userId the patient's user id
     * @return views of the active medication courses
     */
    @Transactional(readOnly = true)
    public List<CourseMedicineView> list(UUID userId) {
        return courseMedicineRepository.findActiveByUserId(userId).stream()
                .map(MedicationService::toView)
                .toList();
    }

    /**
     * Ids of the patient's active courses; used to recheck advice after a profile change or a
     * rules/catalog update. A specific course can then be excluded from its own check.
     *
     * @param userId the patient's user id
     * @return ids of the active medication courses
     */
    @Transactional(readOnly = true)
    public List<UUID> activeCourseIds(UUID userId) {
        return courseMedicineRepository.findActiveByUserId(userId).stream()
                .map(CourseMedicine::getId)
                .toList();
    }

    /**
     * Finds the catalog medicine of a course owned by the given patient.
     *
     * @param userId the patient's user id
     * @param courseIds medication course ids to resolve
     * @return medicine id per owned course, skipping courses that are missing or not owned
     */
    @Transactional(readOnly = true)
    public java.util.Map<UUID, UUID> medicineIdsForCourses(UUID userId, java.util.Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<UUID, UUID> byCourse = new java.util.LinkedHashMap<>();
        for (CourseMedicine course : courseMedicineRepository.findOwnedByIds(userId, courseIds)) {
            byCourse.put(course.getId(), course.getMedicine().getId());
        }
        return byCourse;
    }

    /**
     * Adds a medication course for the patient, resolving the medicine from the catalog when needed.
     *
     * @param userId the patient's user id
     * @param request the course to add
     * @return the stored course as a view
     */
    @Transactional
    public CourseMedicineView add(UUID userId, CourseMedicineRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Medicine medicine = resolveMedicine(request);
        CourseMedicine saved = courseMedicineRepository.save(CourseMedicine.builder()
                .id(UUID.randomUUID())
                .user(user)
                .medicine(medicine)
                .dosage(request.dosage())
                .frequency(request.frequency())
                .startDate(request.startDate())
                .updatedAt(Instant.now())
                .build());
        LOG.debug("User {} added medication course {} ({})", userId, saved.getId(), saved.getMedicine().getName());
        return toView(saved);
    }

    /**
     * Marks the patient's medication course as deleted.
     *
     * @param userId the patient's user id
     * @param courseMedicineId id of the course to delete
     */
    @Transactional
    public void delete(UUID userId, UUID courseMedicineId) {
        CourseMedicine entity = courseMedicineRepository
                .findById(courseMedicineId)
                .filter(course -> course.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Medication not found: " + courseMedicineId));
        entity.markDeleted();
        courseMedicineRepository.save(entity);
        LOG.debug("User {} deleted medication course {}", userId, courseMedicineId);
    }

    private Medicine resolveMedicine(CourseMedicineRequest request) {
        if (request.medicineId() != null) {
            return medicineRepository
                    .findDetailedById(request.medicineId())
                    .orElseThrow(() -> new NotFoundException("Medicine not found: " + request.medicineId()));
        }
        if (request.drugName() == null || request.drugName().isBlank()) {
            throw new BadRequestException("Provide medicineId or drugName");
        }
        return catalogService
                .resolveByName(request.drugName())
                .flatMap(resolved -> resolved.medicineId() != null
                        ? medicineRepository.findById(resolved.medicineId())
                        : java.util.Optional.<Medicine>empty())
                .orElseGet(() -> medicineRepository.save(
                        Medicine.builder().name(request.drugName()).build()));
    }

    private static CourseMedicineView toView(CourseMedicine entity) {
        String brand = entity.getMedicine().getTradeMark() != null
                ? entity.getMedicine().getTradeMark().getNameBrand()
                : null;
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new CourseMedicineView(
                entity.getId(),
                entity.getMedicine().getName(),
                brand,
                entity.getDosage(),
                entity.getFrequency(),
                entity.getStartDate(),
                status,
                entity.getUpdatedAt());
    }
}
