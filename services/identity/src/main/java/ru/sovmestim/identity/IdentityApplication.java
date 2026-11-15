package ru.sovmestim.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Identity service entry point: users, one-time codes and token issuance.
 */
@SpringBootApplication(scanBasePackages = "ru.sovmestim")
@ConfigurationPropertiesScan
public class IdentityApplication {

    private IdentityApplication() {
    }

    /**
     * Starts the identity service.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
