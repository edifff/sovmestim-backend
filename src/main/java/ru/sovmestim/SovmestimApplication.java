package ru.sovmestim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point of the Sovmestim backend (modular monolith).
 *
 * <p>Decision logic lives on the server only: the mobile client holds no rules and no full drug
 * directory (ADR-002). Drug interactions come from {@code InteractionSource} implementations that
 * sit behind an interface so the licensed RLS source can be swapped for a recorded demo source.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class SovmestimApplication {

    public static void main(String[] args) {
        SpringApplication.run(SovmestimApplication.class, args);
    }
}
