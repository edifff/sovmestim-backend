package ru.sovmestim.intake.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.common.sync.ChangeResult;
import ru.sovmestim.intake.client.ResolvedMedicine;
import ru.sovmestim.intake.domain.CourseMedicine;
import ru.sovmestim.intake.domain.Status;
import ru.sovmestim.intake.repository.CourseMedicineRepository;

/**
 * Sync semantics for medication courses: idempotent upserts, tombstones and last-write-wins
 * conflict detection on server time. The interactive flow still uses {@link MedicationService}.
 */
@Service
@RequiredArgsConstructor
public class IntakeSyncService {

    private static final String ID_REQUIRED = "id is required";

    private final CourseMedicineRepository courseMedicineRepository;
    private final MedicationService medicationService;

    /**
     * Creates or updates a medication course from a sync change.
     *
     * @param userId the patient's user id
     * @param id the client-generated course id
     * @param clientUpdatedAt the client modification time, may be {@code null}
     * @param medicineId catalog medicine id, may be {@code null}
     * @param drugName patient-entered name, may be {@code null}
     * @param dosage dosage text
     * @param frequency frequency text
     * @param startDate course start date
     * @param statusName status name
     * @return the outcome and the course to recheck
     */
    @Transactional
    public UpsertResult upsert(
            UUID userId,
            UUID id,
            Instant clientUpdatedAt,
            UUID medicineId,
            String drugName,
            String dosage,
            String frequency,
            LocalDate startDate,
            String statusName) {
        if (id == null) {
            return new UpsertResult(ChangeResult.rejected(null, ID_REQUIRED), null);
        }
        ResolvedMedicine medicine;
        try {
            medicine = medicationService.resolveMedicine(medicineId, drugName);
        } catch (NotFoundException | BadRequestException ex) {
            return new UpsertResult(ChangeResult.rejected(id, ex.getMessage()), null);
        }
        Status status = medicationService.status(statusName);
        Optional<CourseMedicine> existing =
                courseMedicineRepository.findById(id).filter(course -> userId.equals(course.getUserId()));
        return existing
                .map(course -> update(course, clientUpdatedAt, medicine, dosage, frequency, startDate, status))
                .orElseGet(() -> create(userId, id, medicine, dosage, frequency, startDate, status));
    }

    /**
     * Applies a medication tombstone from a sync change.
     *
     * @param userId the patient's user id
     * @param id the course id
     * @return the outcome reported to the client
     */
    @Transactional
    public ChangeResult delete(UUID userId, UUID id) {
        if (id == null) {
            return ChangeResult.rejected(null, ID_REQUIRED);
        }
        Optional<CourseMedicine> existing =
                courseMedicineRepository.findById(id).filter(course -> userId.equals(course.getUserId()));
        if (existing.isEmpty()) {
            return ChangeResult.applied(id, null);
        }
        CourseMedicine entity = existing.get();
        entity.markDeletedSynced();
        return ChangeResult.applied(id, courseMedicineRepository.save(entity).getUpdatedAt());
    }

    /**
     * Returns medication changes after the cursor, including tombstones.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor, or {@code null} to start from the epoch
     * @return the changed courses and the next cursor
     */
    @Transactional(readOnly = true)
    public MedicationDelta changesSince(UUID userId, Instant cursor) {
        Instant from = cursor != null ? cursor : Instant.EPOCH;
        List<CourseMedicine> medications = medicationService.changesSince(userId, from);
        Instant next = from;
        for (CourseMedicine medication : medications) {
            next = later(next, medication.getUpdatedAt());
        }
        return new MedicationDelta(medications, next);
    }

    private UpsertResult update(
            CourseMedicine entity,
            Instant clientUpdatedAt,
            ResolvedMedicine medicine,
            String dosage,
            String frequency,
            LocalDate startDate,
            Status status) {
        if (isServerNewer(entity.getUpdatedAt(), clientUpdatedAt)) {
            return new UpsertResult(ChangeResult.conflict(entity.getId()), null);
        }
        apply(entity, medicine, dosage, frequency, startDate, status);
        CourseMedicine saved = courseMedicineRepository.save(entity);
        return new UpsertResult(ChangeResult.applied(saved.getId(), saved.getUpdatedAt()), saved.getId());
    }

    private UpsertResult create(
            UUID userId,
            UUID id,
            ResolvedMedicine medicine,
            String dosage,
            String frequency,
            LocalDate startDate,
            Status status) {
        CourseMedicine created = courseMedicineRepository.save(CourseMedicine.builder()
                .id(id)
                .userId(userId)
                .medicineId(medicine.id())
                .medicineName(medicine.name())
                .brand(medicine.brand())
                .dosage(dosage)
                .frequency(frequency)
                .startDate(startDate)
                .status(status)
                .synced(true)
                .build());
        return new UpsertResult(ChangeResult.applied(created.getId(), created.getUpdatedAt()), created.getId());
    }

    private static void apply(
            CourseMedicine entity,
            ResolvedMedicine medicine,
            String dosage,
            String frequency,
            LocalDate startDate,
            Status status) {
        entity.setMedicineId(medicine.id());
        entity.setMedicineName(medicine.name());
        entity.setBrand(medicine.brand());
        entity.setDosage(dosage);
        entity.setFrequency(frequency);
        entity.setStartDate(startDate);
        entity.setStatus(status);
        entity.setDeleted(false);
        entity.setSynced(true);
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
