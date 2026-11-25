package ru.sovmestim.intake.service;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.intake.client.CatalogClient;
import ru.sovmestim.intake.client.ResolvedMedicine;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.domain.Status;
import ru.sovmestim.intake.dto.CourseMedicineRequest;
import ru.sovmestim.intake.dto.CourseMedicineView;
import ru.sovmestim.intake.repository.CourseMedicineRepository;
import ru.sovmestim.intake.repository.StatusRepository;
import ru.sovmestim.intake.repository.TakenSubstanceRepository;

/**
 * The patient's current drugs. A name the catalog does not know is stored as a local draft
 * (without a catalog medicine id) and is checked by advice only once it is resolved.
 */
@Service
@RequiredArgsConstructor
public class MedicationService {

    private static final Logger LOG = LoggerFactory.getLogger(MedicationService.class);

    private final CourseMedicineRepository courseMedicineRepository;
    private final CatalogClient catalogClient;
    private final StatusRepository statusRepository;
    private final TakenSubstanceRepository takenSubstanceRepository;

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
     * rules/catalog update.
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
     * @return medicine id per owned course, skipping drafts and unowned courses
     */
    @Transactional(readOnly = true)
    public Map<UUID, UUID> medicineIdsForCourses(UUID userId, Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> byCourse = new LinkedHashMap<>();
        for (CourseMedicine course : courseMedicineRepository.findOwnedByIds(userId, courseIds)) {
            if (course.getMedicineId() != null) {
                byCourse.put(course.getId(), course.getMedicineId());
            }
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
        ResolvedMedicine medicine = resolveMedicine(request.medicineId(), request.drugName());
        CourseMedicine saved = courseMedicineRepository.save(CourseMedicine.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .medicineId(medicine.id())
                .medicineName(medicine.name())
                .brand(medicine.brand())
                .dosage(request.dosage())
                .frequency(request.frequency())
                .startDate(request.startDate())
                .build());
        LOG.debug("User {} added medication course {} ({})", userId, saved.getId(), saved.getMedicineName());
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
        CourseMedicine entity = ownedCourse(userId, courseMedicineId);
        entity.markDeleted();
        courseMedicineRepository.save(entity);
        LOG.debug("User {} deleted medication course {}", userId, courseMedicineId);
    }

    /**
     * Returns medication changes after the cursor, including tombstones.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor, or {@code null} to start from the epoch
     * @return changed courses
     */
    @Transactional(readOnly = true)
    public List<CourseMedicine> changesSince(UUID userId, Instant cursor) {
        return courseMedicineRepository.findChangedSince(
                userId, cursor != null ? cursor : Instant.EPOCH);
    }

    /**
     * Resolves a medicine id or a patient-entered name; an unknown name becomes a local draft.
     *
     * @param medicineId catalog medicine id, may be {@code null}
     * @param drugName patient-entered name, may be {@code null}
     * @return the resolved medicine
     * @throws BadRequestException when neither a medicine id nor a name is given
     */
    public ResolvedMedicine resolveMedicine(UUID medicineId, String drugName) {
        if (medicineId != null) {
            return catalogClient.medicine(medicineId);
        }
        if (drugName == null || drugName.isBlank()) {
            throw new BadRequestException("Provide medicineId or drugName");
        }
        return catalogClient.resolve(drugName).orElseGet(() -> new ResolvedMedicine(null, drugName, null));
    }

    /**
     * Lists the active substance-level intake rules of the patient.
     *
     * @param userId the patient's user id
     * @return the active substance rules that reference a medicine or a substance
     */
    @Transactional(readOnly = true)
    public List<TakenSubstanceRef> activeSubstances(UUID userId) {
        return takenSubstanceRepository.findActiveByUserId(userId).stream()
                .map(rule -> new TakenSubstanceRef(rule.getMedicineId(), rule.getActiveSubstanceId()))
                .filter(ref -> ref.medicineId() != null || ref.activeSubstanceId() != null)
                .toList();
    }

    /**
     * Finds or creates a status by name.
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

    /**
     * Loads a course owned by the patient.
     *
     * @param userId the patient's user id
     * @param courseId the course id
     * @return the owned course
     * @throws NotFoundException when the course is missing or belongs to another patient
     */
    public CourseMedicine ownedCourse(UUID userId, UUID courseId) {
        return courseMedicineRepository
                .findById(courseId)
                .filter(course -> userId.equals(course.getUserId()))
                .orElseThrow(() -> new NotFoundException("Medication not found: " + courseId));
    }

    static CourseMedicineView toView(CourseMedicine entity) {
        String status = entity.getStatus() != null ? entity.getStatus().getName() : null;
        return new CourseMedicineView(
                entity.getId(),
                entity.getMedicineName(),
                entity.getBrand(),
                entity.getDosage(),
                entity.getFrequency(),
                entity.getStartDate(),
                status,
                entity.getUpdatedAt());
    }
}
