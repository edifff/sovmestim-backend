package ru.sovmestim.catalog.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class CatalogImportService {

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
     * Creates the import service.
     *
     * @param atcRepository repository for ATC entries
     * @param substanceRepository repository for active substances
     * @param tradeMarkRepository repository for trade marks
     * @param formReleaseRepository repository for release forms
     * @param unitMeasurementRepository repository for units of measurement
     * @param medicineRepository repository for medicines
     * @param substanceInMedicineRepository repository for medicine-to-substance links
     */
    public CatalogImportService(
            AtcRepository atcRepository,
            ActiveSubstanceRepository substanceRepository,
            TradeMarkRepository tradeMarkRepository,
            FormReleaseRepository formReleaseRepository,
            UnitMeasurementRepository unitMeasurementRepository,
            MedicineRepository medicineRepository,
            SubstanceInMedicineRepository substanceInMedicineRepository) {
        this.atcRepository = atcRepository;
        this.substanceRepository = substanceRepository;
        this.tradeMarkRepository = tradeMarkRepository;
        this.formReleaseRepository = formReleaseRepository;
        this.unitMeasurementRepository = unitMeasurementRepository;
        this.medicineRepository = medicineRepository;
        this.substanceInMedicineRepository = substanceInMedicineRepository;
    }

    /**
     * Imports one catalog snapshot, creating only entries that are not present yet.
     *
     * @param root the normalized snapshot document
     * @return the imported entry counts together with the snapshot version
     */
    @Transactional
    public ImportResult importCatalog(JsonNode root) {
        Map<String, Atc> atcByCode = new LinkedHashMap<>();
        int atcCount = importAtc(root, atcByCode);
        importFormReleases(root);
        importUnitMeasurements(root);
        Map<String, ActiveSubstance> substanceByName = new LinkedHashMap<>();
        int substanceCount = importSubstances(root, atcByCode, substanceByName);
        Map<String, TradeMark> tradeMarkByName = new LinkedHashMap<>();
        importTradeMarks(root, tradeMarkByName);
        int medicineCount = importMedicines(root, substanceByName, tradeMarkByName);
        String version = text(root, "catalog_version");
        return new ImportResult(version, atcCount, substanceCount, medicineCount);
    }

    private int importAtc(JsonNode root, Map<String, Atc> atcByCode) {
        int count = 0;
        for (JsonNode node : root.path(FIELD_ATC)) {
            String code = text(node, FIELD_NAME);
            Atc atc = atcRepository.findByName(code).orElseGet(() -> atcRepository.save(Atc.builder()
                    .name(code)
                    .description(text(node, FIELD_DESCRIPTION))
                    .build()));
            atcByCode.put(code, atc);
            count++;
        }
        return count;
    }

    private void importFormReleases(JsonNode root) {
        for (JsonNode node : root.path("form_releases")) {
            String name = node.asText();
            formReleaseRepository.findByName(name).orElseGet(() -> formReleaseRepository.save(
                    FormRelease.builder().name(name).build()));
        }
    }

    private void importUnitMeasurements(JsonNode root) {
        for (JsonNode node : root.path("unit_measurements")) {
            String name = node.asText();
            unitMeasurementRepository.findByName(name).orElseGet(() -> unitMeasurementRepository.save(
                    UnitMeasurement.builder().name(name).build()));
        }
    }

    private int importSubstances(
            JsonNode root, Map<String, Atc> atcByCode, Map<String, ActiveSubstance> substanceByName) {
        int count = 0;
        for (JsonNode node : root.path(FIELD_SUBSTANCES)) {
            String name = text(node, FIELD_NAME);
            Atc atc = atcByCode.get(text(node, FIELD_ATC));
            ActiveSubstance substance = substanceRepository
                    .findByNameIgnoreCase(name)
                    .orElseGet(() -> substanceRepository.save(ActiveSubstance.builder()
                            .name(name)
                            .atc(atc)
                            .description(text(node, FIELD_DESCRIPTION))
                            .build()));
            substanceByName.put(NameNormalizer.normalize(name), substance);
            count++;
        }
        return count;
    }

    private void importTradeMarks(JsonNode root, Map<String, TradeMark> tradeMarkByName) {
        for (JsonNode node : root.path("trade_marks")) {
            String brand = text(node, "name_brand");
            TradeMark tradeMark = tradeMarkRepository.findByNameBrandIgnoreCase(brand).orElseGet(() -> tradeMarkRepository.save(
                    TradeMark.builder()
                            .nameBrand(brand)
                            .manufacturer(text(node, "manufacturer"))
                            .country(text(node, "country"))
                            .build()));
            tradeMarkByName.put(NameNormalizer.normalize(brand), tradeMark);
        }
    }

    private int importMedicines(
            JsonNode root, Map<String, ActiveSubstance> substanceByName, Map<String, TradeMark> tradeMarkByName) {
        int count = 0;
        for (JsonNode node : root.path("medicines")) {
            String name = text(node, FIELD_NAME);
            if (medicineExists(name)) {
                continue;
            }
            TradeMark tradeMark = tradeMarkByName.get(NameNormalizer.normalize(text(node, "trade_mark")));
            FormRelease form = formReleaseRepository.findByName(text(node, "form_release")).orElse(null);
            Medicine medicine = medicineRepository.save(Medicine.builder()
                    .name(name)
                    .tradeMark(tradeMark)
                    .formRelease(form)
                    .build());
            for (JsonNode component : node.path(FIELD_SUBSTANCES)) {
                ActiveSubstance substance =
                        substanceByName.get(NameNormalizer.normalize(text(component, FIELD_NAME)));
                if (substance == null) {
                    continue;
                }
                UnitMeasurement unit = unitMeasurementRepository
                        .findByName(text(component, "unit"))
                        .orElse(null);
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

    private boolean medicineExists(String name) {
        String normalized = NameNormalizer.normalize(name);
        return medicineRepository.search(normalized, org.springframework.data.domain.PageRequest.of(0, 10)).stream()
                .anyMatch(medicine -> NameNormalizer.matches(medicine.getName(), normalized));
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
