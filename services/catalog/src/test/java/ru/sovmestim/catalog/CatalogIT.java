package ru.sovmestim.catalog;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Catalog service against a real PostgreSQL: migrations, demo seed and search.
 */
@SpringBootTest(properties = {"sovmestim.grpc.port=0", "sovmestim.demo.seed=true"})
class CatalogIT {

    private static final String WARFARIN = "варфарин";

    private static final String DB_USER = "sovmestim";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sovmestim_catalog")
            .withUsername(DB_USER)
            .withPassword(DB_USER);

    static {
        POSTGRES.start();
    }

    @Autowired
    private ru.sovmestim.catalog.service.CatalogService catalogService;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void demoCatalogIsSeededAndSearchable() {
        Assertions.assertThat(catalogService.searchSubstances(WARFARIN, 20)).isNotEmpty();
        Assertions.assertThat(catalogService.searchMedicines(WARFARIN, 20)).isNotEmpty();
    }

    @Test
    void medicineCanBeResolvedByName() {
        var resolved = catalogService.resolveByName(WARFARIN);

        Assertions.assertThat(resolved).isPresent();
        Assertions.assertThat(resolved.orElseThrow().substances()).isNotEmpty();
    }
}
