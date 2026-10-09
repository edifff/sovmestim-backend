package ru.sovmestim.catalog.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.catalog.domain.ActiveSubstance;
import ru.sovmestim.catalog.domain.Atc;
import ru.sovmestim.catalog.domain.FormRelease;
import ru.sovmestim.catalog.domain.Medicine;
import ru.sovmestim.catalog.domain.SubstanceInMedicine;
import ru.sovmestim.catalog.domain.TradeMark;
import ru.sovmestim.catalog.domain.UnitMeasurement;
import ru.sovmestim.catalog.repository.ActiveSubstanceRepository;
import ru.sovmestim.catalog.repository.AtcRepository;
import ru.sovmestim.catalog.repository.FormReleaseRepository;
import ru.sovmestim.catalog.repository.MedicineRepository;
import ru.sovmestim.catalog.repository.SubstanceInMedicineRepository;
import ru.sovmestim.catalog.repository.TradeMarkRepository;
import ru.sovmestim.catalog.repository.UnitMeasurementRepository;
import ru.sovmestim.common.util.NameNormalizer;
import tools.jackson.databind.JsonNode;

/**
 * Imports a normalized catalog snapshot (ATC, active substances, trade marks, medicines) into the
 * {@code catalog} tables. In production this is fed by the scheduled RLS dictionary import; in demo
 * mode it is fed by the recorded snapshot. The operation is idempotent.
 */
@Service
@RequiredArgsConstructor
public class CatalogImportService {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogImportService.class);

    /** Snapshot JSON field holding the ATC block. */
    private static final String FIELD_ATC = "atc";

    /** Snapshot JSON field holding a display name. */
    private static final String FIELD_NAME = "name";

    /** Snapshot JSON field holding a description. */
    private static final String FIELD_DESCRIPTION = "description";

    /** Snapshot JSON field holding the active substances block. */
    private static final String FIELD_SUBSTANCES = "substances";

    private final AtcRepository atcRepository;
    private final ActiveSubstanceRepository substanceRepository;
    private final TradeMarkRepository tradeMarkRepository;
    private final FormReleaseRepository formReleaseRepository;
    private final UnitMeasurementRepository unitMeasurementRepository;
    private final MedicineRepository medicineRepository;
    private final SubstanceInMedicineRepository substanceInMedicineRepository;

    /**
     * Imports one catalog snapshot, creating only entries that are not present yet.
     *
     * @param root the normalized snapshot document
     * @return the imported entry counts together with the snapshot version
     */
    @Transactional
    @CacheEvict(cacheNames = {"catalogMedicines", "catalogMedicineSubstances"}, allEntries = true)
    public ImportResult importCatalog(JsonNode root) {
        // Existing rows are loaded once into maps so the import loops never issue a query per item.
        Map<String, Atc> existingAtc = atcRepository.findAll().stream()
                .collect(Collectors.toMap(atc -> atc.getName(), atc -> atc, (first, second) -> first, LinkedHashMap::new));
        Map<String, ActiveSubstance> existingSubstances = substanceRepository.findAll().stream()
                .collect(Collectors.toMap(
                        substance -> substance.getName().toLowerCase(Locale.ROOT),
                        substance -> substance,
                        (first, second) -> first,
                        LinkedHashMap::new));
        Map<String, TradeMark> existingTradeMarks = tradeMarkRepository.findAll().stream()
                .collect(Collectors.toMap(
                        tradeMark -> tradeMark.getNameBrand().toLowerCase(Locale.ROOT),
                        tradeMark -> tradeMark,
                        (first, second) -> first,
                        LinkedHashMap::new));
        Map<String, FormRelease> existingForms = formReleaseRepository.findAll().stream()
                .collect(Collectors.toMap(FormRelease::getName, form -> form, (first, second) -> first, LinkedHashMap::new));
        Map<String, UnitMeasurement> existingUnits = unitMeasurementRepository.findAll().stream()
                .collect(Collectors.toMap(UnitMeasurement::getName, unit -> unit, (first, second) -> first, LinkedHashMap::new));
        Map<String, Medicine> existingMedicines = new LinkedHashMap<>();

        Map<String, Atc> atcByCode = new LinkedHashMap<>();
        int atcCount = importAtc(root, existingAtc, atcByCode);
        importFormReleases(root, existingForms);
        importUnitMeasurements(root, existingUnits);
        Map<String, ActiveSubstance> substanceByName = new LinkedHashMap<>();
        int substanceCount = importSubstances(root, atcByCode, existingSubstances, substanceByName);
        Map<String, TradeMark> tradeMarkByName = new LinkedHashMap<>();
        importTradeMarks(root, existingTradeMarks, tradeMarkByName);
        int medicineCount = importMedicines(root, substanceByName, tradeMarkByName, existingForms, existingUnits,
                existingMedicines);
        String version = text(root, "catalog_version");
        LOG.info(
                "Catalog import finished: version={}, atc={}, substances={}, medicines={}",
                version,
                atcCount,
                substanceCount,
                medicineCount);
        return new ImportResult(version, atcCount, substanceCount, medicineCount);
    }

    private int importAtc(JsonNode root, Map<String, Atc> existingAtc, Map<String, Atc> atcByCode) {
        int count = 0;
        for (JsonNode node : root.path(FIELD_ATC)) {
            String code = text(node, FIELD_NAME);
            Atc atc = existingAtc.computeIfAbsent(code, key -> atcRepository.save(Atc.builder()
                    .name(code)
                    .description(text(node, FIELD_DESCRIPTION))
                    .build()));
            atcByCode.put(code, atc);
            count++;
        }
        return count;
    }

    private void importFormReleases(JsonNode root, Map<String, FormRelease> existingForms) {
        for (JsonNode node : root.path("form_releases")) {
            String name = node.asText();
            existingForms.computeIfAbsent(name, key -> formReleaseRepository.save(
                    FormRelease.builder().name(name).build()));
        }
    }

    private void importUnitMeasurements(JsonNode root, Map<String, UnitMeasurement> existingUnits) {
        for (JsonNode node : root.path("unit_measurements")) {
            String name = node.asText();
            existingUnits.computeIfAbsent(name, key -> unitMeasurementRepository.save(
                    UnitMeasurement.builder().name(name).build()));
        }
    }

    private int importSubstances(
            JsonNode root,
            Map<String, Atc> atcByCode,
            Map<String, ActiveSubstance> existingSubstances,
            Map<String, ActiveSubstance> substanceByName) {
        int count = 0;
        for (JsonNode node : root.path(FIELD_SUBSTANCES)) {
            String name = text(node, FIELD_NAME);
            Atc atc = atcByCode.get(text(node, FIELD_ATC));
            ActiveSubstance substance = existingSubstances.computeIfAbsent(
                    name.toLowerCase(Locale.ROOT), key -> substanceRepository.save(ActiveSubstance.builder()
                            .name(name)
                            .atc(atc)
                            .description(text(node, FIELD_DESCRIPTION))
                            .build()));
            substanceByName.put(NameNormalizer.normalize(name), substance);
            count++;
        }
        return count;
    }

    private void importTradeMarks(
            JsonNode root, Map<String, TradeMark> existingTradeMarks, Map<String, TradeMark> tradeMarkByName) {
        for (JsonNode node : root.path("trade_marks")) {
            String brand = text(node, "name_brand");
            TradeMark tradeMark = existingTradeMarks.computeIfAbsent(
                    brand.toLowerCase(Locale.ROOT), key -> tradeMarkRepository.save(TradeMark.builder()
                            .nameBrand(brand)
                            .manufacturer(text(node, "manufacturer"))
                            .country(text(node, "country"))
                            .build()));
            tradeMarkByName.put(NameNormalizer.normalize(brand), tradeMark);
        }
    }

    private int importMedicines(
            JsonNode root,
            Map<String, ActiveSubstance> substanceByName,
            Map<String, TradeMark> tradeMarkByName,
            Map<String, FormRelease> existingForms,
            Map<String, UnitMeasurement> existingUnits,
            Map<String, Medicine> existingMedicines) {
        int count = 0;
        for (JsonNode node : root.path("medicines")) {
            String name = text(node, FIELD_NAME);
            if (medicineExists(name, existingMedicines)) {
                continue;
            }
            TradeMark tradeMark = tradeMarkByName.get(NameNormalizer.normalize(text(node, "trade_mark")));
            FormRelease form = existingForms.get(text(node, "form_release"));
            Medicine medicine = medicineRepository.save(Medicine.builder()
                    .name(name)
                    .tradeMark(tradeMark)
                    .formRelease(form)
                    .build());
            existingMedicines.put(NameNormalizer.normalize(name), medicine);
            for (JsonNode component : node.path(FIELD_SUBSTANCES)) {
                ActiveSubstance substance =
                        substanceByName.get(NameNormalizer.normalize(text(component, FIELD_NAME)));
                if (substance == null) {
                    continue;
                }
                UnitMeasurement unit = existingUnits.get(text(component, "unit"));
                substanceInMedicineRepository.save(SubstanceInMedicine.builder()
                        .medicine(medicine)
                        .activeSubstance(substance)
                        .unitMeasurement(unit)
                        .dosage(text(component, "dosage"))
                        .build());
            }
            count++;
        }
        return count;
    }

    private boolean medicineExists(String name, Map<String, Medicine> existingMedicines) {
        String normalized = NameNormalizer.normalize(name);
        Medicine medicine = existingMedicines.get(normalized);
        return medicine != null && NameNormalizer.matches(medicine.getName(), normalized);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Counts and version produced by one catalog import run.
     *
     * @param catalogVersion the version reported by the imported snapshot
     * @param atc the number of ATC entries processed
     * @param substances the number of active substances processed
     * @param medicines the number of medicines created
     */
    public record ImportResult(String catalogVersion, int atc, int substances, int medicines) { }
}
