package ru.sovmestim.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Catalog service entry point: drug dictionary, search and name resolution.
 */
@SpringBootApplication(scanBasePackages = "ru.sovmestim")
@ConfigurationPropertiesScan
@EnableCaching
public class CatalogApplication {

    private CatalogApplication() {
    }

    /**
     * Starts the catalog service.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
