package ru.sovmestim.catalog.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.sovmestim.catalog.repository.ActiveSubstanceRepository;
import ru.sovmestim.catalog.repository.AtcRepository;
import ru.sovmestim.catalog.repository.FormReleaseRepository;
import ru.sovmestim.catalog.repository.MedicineRepository;
import ru.sovmestim.catalog.repository.SubstanceInMedicineRepository;
import ru.sovmestim.catalog.repository.TradeMarkRepository;
import ru.sovmestim.catalog.repository.UnitMeasurementRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tolerance of the catalog importer towards incomplete snapshot entries.
 */
class CatalogImportServiceTest {

    private final ActiveSubstanceRepository substanceRepository = Mockito.mock(ActiveSubstanceRepository.class);

    private final TradeMarkRepository tradeMarkRepository = Mockito.mock(TradeMarkRepository.class);

    private final CatalogImportService service = new CatalogImportService(
            Mockito.mock(AtcRepository.class),
            substanceRepository,
            tradeMarkRepository,
            Mockito.mock(FormReleaseRepository.class),
            Mockito.mock(UnitMeasurementRepository.class),
            Mockito.mock(MedicineRepository.class),
            Mockito.mock(SubstanceInMedicineRepository.class));

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void skipsSubstanceWithoutName() {
        JsonNode root = mapper.readTree("""
                {"catalog_version": "test", "substances": [{"atc": "A", "description": "no name"}]}
                """);

        ImportResult result = service.importCatalog(root);

        Assertions.assertThat(result.substances()).isZero();
        Mockito.verify(substanceRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void skipsTradeMarkWithoutBrand() {
        JsonNode root = mapper.readTree("""
                {"catalog_version": "test", "trade_marks": [{"manufacturer": "no brand"}]}
                """);

        service.importCatalog(root);

        Mockito.verify(tradeMarkRepository, Mockito.never()).save(Mockito.any());
    }
}
