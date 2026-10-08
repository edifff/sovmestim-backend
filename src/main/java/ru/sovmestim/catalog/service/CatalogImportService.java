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

    private final AtcRepository atcRepository;
    private final ActiveSubstanceRepository substanceRepository;
    private final TradeMarkRepository tradeMarkRepository;
    private final FormReleaseRepository formReleaseRepository;
    private final UnitMeasurementRepository unitMeasurementRepository;
    private final MedicineRepository medicineRepository;
    private final SubstanceInMedicineRepository substanceInMedicineRepository;

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

    @Transactional
    public ImportResult importCatalog(JsonNode root) {
        int atcCount = 0;
        int substanceCount = 0;
        int medicineCount = 0;

        Map<String, Atc> atcByCode = new LinkedHashMap<>();
        for (JsonNode node : root.path("atc")) {
            String code = text(node, "name");
            Atc atc = atcRepository.findByName(code).orElseGet(() -> atcRepository.save(Atc.builder()
                    .name(code)
                    .description(text(node, "description"))
                    .build()));
            atcByCode.put(code, atc);
            atcCount++;
        }

        for (JsonNode node : root.path("form_releases")) {
            String name = node.asText();
            formReleaseRepository.findByName(name).orElseGet(() -> formReleaseRepository.save(
                    FormRelease.builder().name(name).build()));
        }
        for (JsonNode node : root.path("unit_measurements")) {
            String name = node.asText();
            unitMeasurementRepository.findByName(name).orElseGet(() -> unitMeasurementRepository.save(
                    UnitMeasurement.builder().name(name).build()));
        }

        Map<String, ActiveSubstance> substanceByName = new LinkedHashMap<>();
        for (JsonNode node : root.path("substances")) {
            String name = text(node, "name");
            Atc atc = atcByCode.get(text(node, "atc"));
            ActiveSubstance substance = substanceRepository
                    .findByNameIgnoreCase(name)
                    .orElseGet(() -> substanceRepository.save(ActiveSubstance.builder()
                            .name(name)
                            .atc(atc)
                            .description(text(node, "description"))
                            .build()));
            substanceByName.put(NameNormalizer.normalize(name), substance);
            substanceCount++;
        }

        Map<String, TradeMark> tradeMarkByName = new LinkedHashMap<>();
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

        for (JsonNode node : root.path("medicines")) {
            String name = text(node, "name");
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
            for (JsonNode component : node.path("substances")) {
                ActiveSubstance substance =
                        substanceByName.get(NameNormalizer.normalize(text(component, "name")));
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
            medicineCount++;
        }

        String version = text(root, "catalog_version");
        return new ImportResult(version, atcCount, substanceCount, medicineCount);
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

    public record ImportResult(String catalogVersion, int atc, int substances, int medicines) {}
}
