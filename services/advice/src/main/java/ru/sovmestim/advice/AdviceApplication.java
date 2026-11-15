package ru.sovmestim.advice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Advice service entry point: interaction sources, the compatibility engine and advice audit.
 */
@SpringBootApplication(scanBasePackages = "ru.sovmestim")
@ConfigurationPropertiesScan
@EnableCaching
public class AdviceApplication {

    private AdviceApplication() {
    }

    /**
     * Starts the advice service.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(AdviceApplication.class, args);
    }
}
