package ru.sovmestim.catalog.service;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.sovmestim.advice.domain.DangerLevel;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.repository.DangerLevelRepository;
import ru.sovmestim.catalog.repository.ActiveSubstanceRepository;
import ru.sovmestim.config.DemoProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Seeds the demo catalog and danger-level dictionary on startup. This is what makes a presentation
 * work with zero paid RLS calls; it is a no-op when {@code sovmestim.demo.seed=false}.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_CATALOG = "classpath:demo/catalog.json";

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoProperties demoProperties;
    private final CatalogImportService catalogImportService;
    private final ActiveSubstanceRepository substanceRepository;
    private final DangerLevelRepository dangerLevelRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    /**
     * Creates the seeder.
     *
     * @param demoProperties demo mode configuration
     * @param catalogImportService importer for the catalog snapshot
     * @param substanceRepository repository used to detect an already populated catalog
     * @param dangerLevelRepository repository for the danger level dictionary
     * @param resourceLoader loader for the demo catalog resource
     * @param objectMapper JSON mapper used to read the demo snapshot
     */
    public DemoDataSeeder(
            DemoProperties demoProperties,
            CatalogImportService catalogImportService,
            ActiveSubstanceRepository substanceRepository,
            DangerLevelRepository dangerLevelRepository,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.demoProperties = demoProperties;
        this.catalogImportService = catalogImportService;
        this.substanceRepository = substanceRepository;
        this.dangerLevelRepository = dangerLevelRepository;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedDangerLevels();
        if (!demoProperties.seed()) {
            return;
        }
        if (substanceRepository.count() > 0) {
            LOG.info("Catalog already populated, skipping demo seed");
            return;
        }
        Resource resource = resourceLoader.getResource(DEMO_CATALOG);
        try (InputStream input = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            CatalogImportService.ImportResult result = catalogImportService.importCatalog(root);
            LOG.info(
                    "Demo catalog imported ({}): {} ATC, {} substances, {} medicines",
                    result.catalogVersion(),
                    result.atc(),
                    result.substances(),
                    result.medicines());
        }
    }

    private void seedDangerLevels() {
        for (AdviceLevel level : AdviceLevel.values()) {
            dangerLevelRepository
                    .findByNameIgnoreCase(level.name())
                    .orElseGet(() -> dangerLevelRepository.save(
                            DangerLevel.builder().name(level.name()).build()));
        }
    }
}
