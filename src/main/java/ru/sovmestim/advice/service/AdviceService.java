package ru.sovmestim.advice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.sovmestim.advice.domain.AdviceRecord;
import ru.sovmestim.advice.dto.AdviceCheckRequest;
import ru.sovmestim.advice.dto.AdviceCheckResponse;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.model.DrugRef;
import ru.sovmestim.advice.model.PatientSnapshot;
import ru.sovmestim.advice.repository.AdviceRecordRepository;
import ru.sovmestim.advice.source.DemoInteractionSource;
import ru.sovmestim.catalog.service.CatalogService;
import ru.sovmestim.catalog.service.ResolvedDrug;
import ru.sovmestim.common.error.BadRequestException;
import ru.sovmestim.intake.service.MedicationService;
import ru.sovmestim.patient.service.PatientSnapshotService;
import tools.jackson.databind.ObjectMapper;

/**
 * Application service that resolves the drug, builds the patient snapshot, runs the engine and
 * stores an immutable audit row.
 */
@Service
public class AdviceService {

    private final CatalogService catalogService;
    private final PatientSnapshotService patientSnapshotService;
    private final AdviceEngine adviceEngine;
    private final AdviceRecordRepository adviceRecordRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<DemoInteractionSource> demoInteractionSource;
    private final MedicationService medicationService;

    /**
     * Creates the service over the catalog, snapshot, engine and audit persistence.
     *
     * @param catalogService catalog used to resolve the checked drug
     * @param patientSnapshotService builds the patient snapshot at check time
     * @param adviceEngine decision engine
     * @param adviceRecordRepository persists audit rows
     * @param objectMapper serializes request and result to JSON
     * @param demoInteractionSource optional demo source providing the catalog version
     * @param medicationService provides active courses for rechecks
     */
    public AdviceService(
            CatalogService catalogService,
            PatientSnapshotService patientSnapshotService,
            AdviceEngine adviceEngine,
            AdviceRecordRepository adviceRecordRepository,
            ObjectMapper objectMapper,
            ObjectProvider<DemoInteractionSource> demoInteractionSource,
            MedicationService medicationService) {
        this.catalogService = catalogService;
        this.patientSnapshotService = patientSnapshotService;
        this.adviceEngine = adviceEngine;
        this.adviceRecordRepository = adviceRecordRepository;
        this.objectMapper = objectMapper;
        this.demoInteractionSource = demoInteractionSource;
        this.medicationService = medicationService;
    }

    /**
     * Runs the advice check and stores an audit record.
     *
     * @param userId id of the requesting user
     * @param request drug identification for the check
     * @return stored advice response
     */
    @Transactional
    public AdviceCheckResponse check(UUID userId, AdviceCheckRequest request) {
        return check(userId, request, null);
    }

    private AdviceCheckResponse check(UUID userId, AdviceCheckRequest request, UUID excludeCourseMedicineId) {
        ResolvedDrug drug = resolve(request);
        PatientSnapshot snapshot = patientSnapshotService.snapshot(userId, excludeCourseMedicineId);
        String catalogVersion = catalogVersion();

        AdviceResult result = adviceEngine.check(
                snapshot, new DrugRef(drug.medicineId(), drug.name(), drug.substances()), catalogVersion);

        AdviceRecord record = AdviceRecord.builder()
                .userId(userId)
                .drugName(drug.name())
                .status(result.status().name())
                .level(result.level() != null ? result.level().name() : null)
                .rulesVersion(result.rulesVersion())
                .mappingVersion(result.mappingVersion())
                .catalogVersion(result.catalogVersion())
                .requestJson(write(request))
                .resultJson(write(result))
                .createdAt(Instant.now())
                .build();
        adviceRecordRepository.save(record);
        return new AdviceCheckResponse(record.getId(), result);
    }

    /**
     * Recomputes advice for the given active courses and stores fresh records. Each course is
     * excluded from its own snapshot so it is not reported as a duplicate of itself. Called after
     * sync push and when rules/catalog change.
     *
     * @param userId id of the requesting user
     * @param courseIds active course ids to recheck
     * @return one stored response per rechecked course
     */
    @Transactional
    public List<AdviceCheckResponse> recheckCourses(UUID userId, Collection<UUID> courseIds) {
        List<AdviceCheckResponse> responses = new ArrayList<>();
        for (UUID courseId : courseIds) {
            medicineIdForCourse(userId, courseId).ifPresent(medicineId -> responses.add(
                    check(userId, new AdviceCheckRequest(null, medicineId, null), courseId)));
        }
        return responses;
    }

    /**
     * Recomputes advice for every active course of the user (rules/catalog change trigger).
     *
     * @param userId id of the requesting user
     * @return one stored response per active course
     */
    @Transactional
    public List<AdviceCheckResponse> recheckAll(UUID userId) {
        return recheckCourses(userId, medicationService.activeCourseIds(userId));
    }

    private java.util.Optional<UUID> medicineIdForCourse(UUID userId, UUID courseId) {
        return medicationService.medicineIdForCourse(courseId, userId);
    }

    private ResolvedDrug resolve(AdviceCheckRequest request) {
        if (request.medicineId() != null) {
            var medicine = catalogService.getMedicine(request.medicineId());
            return new ResolvedDrug(
                    medicine.id(), medicine.name(), catalogService.substancesForMedicine(medicine.id()));
        }
        if (request.drugName() != null && !request.drugName().isBlank()) {
            return catalogService
                    .resolveByName(request.drugName())
                    .orElseGet(() -> new ResolvedDrug(null, request.drugName(), List.of()));
        }
        if (request.substanceIds() != null && !request.substanceIds().isEmpty()) {
            List<ru.sovmestim.advice.model.SubstanceRef> substances =
                    catalogService.substancesByIds(request.substanceIds());
            if (substances.isEmpty()) {
                throw new BadRequestException("No known substances for the given ids");
            }
            return new ResolvedDrug(null, substances.get(0).name(), substances);
        }
        throw new BadRequestException("Provide medicineId, drugName or substanceIds");
    }

    private String catalogVersion() {
        DemoInteractionSource demo = demoInteractionSource.getIfAvailable();
        return demo != null ? demo.catalogVersion() : "unknown";
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
