package ru.sovmestim.advice.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.client.CatalogClient;
import ru.sovmestim.advice.client.IntakeClient;
import ru.sovmestim.advice.client.MedicineInfo;
import ru.sovmestim.advice.client.ResolvedDrug;
import ru.sovmestim.advice.domain.AdviceRecord;
import ru.sovmestim.advice.dto.AdviceCheckRequest;
import ru.sovmestim.advice.dto.AdviceCheckResponse;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.model.DrugRef;
import ru.sovmestim.advice.model.PatientSnapshot;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.advice.repository.AdviceRecordRepository;
import ru.sovmestim.advice.source.DemoInteractionSource;
import ru.sovmestim.common.error.BadRequestException;
import tools.jackson.databind.ObjectMapper;

/**
 * Application service that resolves the drug, builds the patient snapshot, runs the engine and
 * stores an immutable audit row.
 */
@Service
@RequiredArgsConstructor
public class AdviceService {

    private static final Logger LOG = LoggerFactory.getLogger(AdviceService.class);

    private final CatalogClient catalogClient;
    private final IntakeClient intakeClient;
    private final PatientSnapshotService patientSnapshotService;
    private final AdviceEngine adviceEngine;
    private final AdviceRecordRepository adviceRecordRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<DemoInteractionSource> demoInteractionSource;
    private final TransactionTemplate auditTransaction;
    private final Clock clock;

    /**
     * Runs the advice check and stores an audit record.
     *
     * <p>Deliberately not transactional as a whole: the interaction sources may perform a remote
     * RLS call, and holding a database connection while waiting on the network would tie up the
     * pool. The audit row is written in its own short transaction.
     *
     * @param userId id of the requesting user
     * @param request drug identification for the check
     * @return stored advice response
     */
    public AdviceCheckResponse check(UUID userId, AdviceCheckRequest request) {
        return check(userId, request, null);
    }

    private AdviceCheckResponse check(UUID userId, AdviceCheckRequest request, UUID excludeCourseMedicineId) {
        ResolvedDrug drug = resolve(request);
        PatientSnapshot snapshot = patientSnapshotService.snapshot(userId, excludeCourseMedicineId);
        String catalogVersion = catalogVersion();

        AdviceResult result = adviceEngine.check(
                snapshot, new DrugRef(drug.medicineId(), drug.name(), drug.substances()), catalogVersion);
        LOG.info(
                "Advice check for user {}: status={}, level={}, findings={}",
                userId,
                result.status(),
                result.level(),
                result.findings().size());

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
                .createdAt(clock.instant())
                .build();
        auditTransaction.executeWithoutResult(status -> adviceRecordRepository.save(record));
        return new AdviceCheckResponse(record.getId(), result);
    }

    /**
     * Recomputes advice for the given active courses and stores fresh records. Each course is
     * excluded from its own snapshot so it is not reported as a duplicate of itself.
     *
     * @param userId id of the requesting user
     * @param courseIds active course ids to recheck
     * @return one stored response per rechecked course
     */
    public List<AdviceCheckResponse> recheckCourses(UUID userId, Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        java.util.Map<UUID, UUID> medicineByCourse = intakeClient.medicineIdsForCourses(userId, courseIds);
        List<AdviceCheckResponse> responses = new ArrayList<>();
        for (UUID courseId : courseIds) {
            UUID medicineId = medicineByCourse.get(courseId);
            if (medicineId != null) {
                responses.add(check(userId, new AdviceCheckRequest(null, medicineId, null), courseId));
            }
        }
        return responses;
    }

    /**
     * Recomputes advice for every active course of the user (rules/catalog change trigger).
     *
     * @param userId id of the requesting user
     * @return one stored response per active course
     */
    public List<AdviceCheckResponse> recheckAll(UUID userId) {
        return recheckCourses(userId, intakeClient.activeCourseIds(userId));
    }

    /**
     * Returns stored advice after the cursor, decoded for delivery.
     *
     * @param userId id of the requesting user
     * @param cursor exclusive change cursor, or {@code null} to start from the epoch
     * @return the advice records and the next cursor
     */
    @Transactional(readOnly = true)
    public AdviceDelta adviceSince(UUID userId, Instant cursor) {
        Instant from = cursor != null ? cursor : Instant.EPOCH;
        List<AdviceRecord> records =
                adviceRecordRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, from);
        List<AdviceDeliveryView> deliveries = new ArrayList<>();
        Instant next = from;
        for (AdviceRecord record : records) {
            AdviceResult result = readResult(record);
            if (result != null) {
                deliveries.add(new AdviceDeliveryView(
                        record.getId().toString(), record.getDrugName(), record.getCreatedAt(), result));
            }
            if (record.getCreatedAt() != null && record.getCreatedAt().isAfter(next)) {
                next = record.getCreatedAt();
            }
        }
        return new AdviceDelta(deliveries, next);
    }

    private ResolvedDrug resolve(AdviceCheckRequest request) {
        if (request.medicineId() != null) {
            MedicineInfo medicine = catalogClient.medicine(request.medicineId());
            return new ResolvedDrug(medicine.id(), medicine.name(), medicine.substances());
        }
        if (request.drugName() != null && !request.drugName().isBlank()) {
            return catalogClient
                    .resolve(request.drugName())
                    .orElseGet(() -> new ResolvedDrug(null, request.drugName(), List.of()));
        }
        if (request.substanceIds() != null && !request.substanceIds().isEmpty()) {
            List<SubstanceRef> substances = catalogClient.substancesByIds(request.substanceIds());
            if (substances.isEmpty()) {
                throw new BadRequestException("No known substances for the given ids");
            }
            return new ResolvedDrug(null, substances.getFirst().name(), substances);
        }
        throw new BadRequestException("Provide medicineId, drugName or substanceIds");
    }

    private String catalogVersion() {
        DemoInteractionSource demo = demoInteractionSource.getIfAvailable();
        return demo != null ? demo.catalogVersion() : "unknown";
    }

    private AdviceResult readResult(AdviceRecord record) {
        if (record.getResultJson() == null || record.getResultJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(record.getResultJson(), AdviceResult.class);
        } catch (Exception ex) {
            LOG.warn("Cannot deserialize stored advice result {}", record.getId(), ex);
            return null;
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            LOG.warn("Cannot serialize {} for the audit record", value.getClass().getSimpleName(), ex);
            return null;
        }
    }
}
