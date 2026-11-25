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

import lombok.RequiredArgsConstructor;

import ru.sovmestim.catalog.config.DemoProperties;
import ru.sovmestim.catalog.repository.ActiveSubstanceRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Seeds the demo catalog on startup. This is what makes a presentation work with zero paid RLS
 * calls; it is a no-op when {@code sovmestim.demo.seed=false}.
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_CATALOG = "classpath:demo/catalog.json";

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoProperties demoProperties;
    private final CatalogImportService catalogImportService;
    private final ActiveSubstanceRepository substanceRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
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
            ImportResult result = catalogImportService.importCatalog(root);
            LOG.info(
                    "Demo catalog imported ({}): {} ATC, {} substances, {} medicines",
                    result.catalogVersion(),
                    result.atc(),
                    result.substances(),
                    result.medicines());
        }
    }

}
