package ru.sovmestim.catalog.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.catalog.domain.ActiveSubstance;
import ru.sovmestim.catalog.domain.Medicine;
import ru.sovmestim.catalog.domain.SubstanceInMedicine;
import ru.sovmestim.catalog.dto.MedicineView;
import ru.sovmestim.catalog.dto.SubstanceDoseView;
import ru.sovmestim.catalog.dto.SubstanceView;
import ru.sovmestim.catalog.repository.ActiveSubstanceRepository;
import ru.sovmestim.catalog.repository.MedicineRepository;
import ru.sovmestim.catalog.repository.SubstanceInMedicineRepository;
import ru.sovmestim.common.error.NotFoundException;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Read access to the drug catalog plus name normalization and drug-to-substance resolution.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogService {

    private static final int MAX_LIMIT = 50;

    private final ActiveSubstanceRepository substanceRepository;
    private final MedicineRepository medicineRepository;
    private final SubstanceInMedicineRepository substanceInMedicineRepository;

    /**
     * Searches active substances by name.
     *
     * @param query the name fragment to search for
     * @param limit the maximum number of results
     * @return matching substances
     */
    public List<SubstanceView> searchSubstances(String query, int limit) {
        String normalized = NameNormalizer.normalize(query);
        if (normalized.isBlank()) {
            return List.of();
        }
        return substanceRepository.search(normalized, PageRequest.of(0, clamp(limit))).stream()
                .map(CatalogService::toSubstanceView)
                .toList();
    }

    /**
     * Searches medicines by name or trade mark name.
     *
     * @param query the name fragment to search for
     * @param limit the maximum number of results
     * @return matching medicines
     */
    public List<MedicineView> searchMedicines(String query, int limit) {
        String normalized = NameNormalizer.normalize(query);
        if (normalized.isBlank()) {
            return List.of();
        }
        List<Medicine> medicines = medicineRepository.search(normalized, PageRequest.of(0, clamp(limit)));
        return toMedicineViews(medicines);
    }

    /**
     * Loads one medicine with all its substances.
     *
     * @param id the medicine identifier
     * @return the medicine view
     */
    @Cacheable("catalogMedicines")
    public MedicineView getMedicine(UUID id) {
        Medicine medicine = medicineRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Medicine not found: " + id));
        return toMedicineViews(List.of(medicine)).get(0);
    }

    /**
     * Resolves an entered drug name either to a medicine (including all its substances) or, when no
     * medicine matches, to a single active substance.
     *
     * @param rawName the drug name as entered by the patient
     * @return the resolution result, or empty when nothing matches
     */
    public Optional<ResolvedDrug> resolveByName(String rawName) {
        String normalized = NameNormalizer.normalize(rawName);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        for (Medicine medicine : medicineRepository.search(normalized, PageRequest.of(0, 10))) {
            String brand = medicine.getTradeMark() != null ? medicine.getTradeMark().getNameBrand() : null;
            if (NameNormalizer.matches(medicine.getName(), normalized) || NameNormalizer.matches(brand, normalized)) {
                return Optional.of(new ResolvedDrug(medicine.getId(), medicine.getName(), substancesOf(medicine.getId())));
            }
        }
        for (ActiveSubstance substance : substanceRepository.search(normalized, PageRequest.of(0, 10))) {
            if (NameNormalizer.matches(substance.getName(), normalized)) {
                return Optional.of(
                        new ResolvedDrug(null, substance.getName(), List.of(toSubstanceRef(substance))));
            }
        }
        return Optional.empty();
    }

    /**
     * Loads substance references for the given substance identifiers.
     *
     * @param ids the substance identifiers, may be null or empty
     * @return matching substance references
     */
    public List<SubstanceRef> substancesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return substanceRepository.findAllByIdIn(ids).stream().map(CatalogService::toSubstanceRef).toList();
    }

    /**
     * Loads the substances of one medicine.
     *
     * @param medicineId the medicine identifier
     * @return substance references of the medicine
     */
    @Cacheable("catalogMedicineSubstances")
    public List<SubstanceRef> substancesForMedicine(UUID medicineId) {
        return substancesOf(medicineId);
    }

    private List<SubstanceRef> substancesOf(UUID medicineId) {
        return substanceInMedicineRepository.findByMedicineId(medicineId).stream()
                .map(link -> toSubstanceRef(link.getActiveSubstance()))
                .toList();
    }

    private List<MedicineView> toMedicineViews(List<Medicine> medicines) {
        if (medicines.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = medicines.stream().map(Medicine::getId).toList();
        Map<UUID, List<SubstanceInMedicine>> byMedicine = new LinkedHashMap<>();
        for (SubstanceInMedicine link : substanceInMedicineRepository.findByMedicineIdIn(ids)) {
            byMedicine.computeIfAbsent(link.getMedicine().getId(), key -> new ArrayList<>()).add(link);
        }
        return medicines.stream()
                .map(medicine -> {
                    List<SubstanceDoseView> substances = byMedicine
                            .getOrDefault(medicine.getId(), List.of())
                            .stream()
                            .map(link -> new SubstanceDoseView(
                                    link.getActiveSubstance().getId(),
                                    link.getActiveSubstance().getName(),
                                    link.getDosage()))
                            .toList();
                    String brand =
                            medicine.getTradeMark() != null ? medicine.getTradeMark().getNameBrand() : null;
                    String form = medicine.getFormRelease() != null ? medicine.getFormRelease().getName() : null;
                    return new MedicineView(medicine.getId(), medicine.getName(), brand, form, substances);
                })
                .toList();
    }

    private static SubstanceView toSubstanceView(ActiveSubstance substance) {
        String atcCode = substance.getAtc() != null ? substance.getAtc().getName() : null;
        String atcName = substance.getAtc() != null ? substance.getAtc().getDescription() : null;
        return new SubstanceView(substance.getId(), substance.getName(), atcCode, atcName, substance.getDescription());
    }

    private static SubstanceRef toSubstanceRef(ActiveSubstance substance) {
        String atcCode = substance.getAtc() != null ? substance.getAtc().getName() : null;
        String atcName = substance.getAtc() != null ? substance.getAtc().getDescription() : null;
        return new SubstanceRef(substance.getId(), substance.getName(), atcCode, atcName, null);
    }

    private static int clamp(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
