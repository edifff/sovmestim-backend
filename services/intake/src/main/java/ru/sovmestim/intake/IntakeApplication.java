package ru.sovmestim.intake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Intake service entry point: current medication courses.
 */
@SpringBootApplication(scanBasePackages = "ru.sovmestim")
public class IntakeApplication {

    private IntakeApplication() {
    }

    /**
     * Starts the intake service.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(IntakeApplication.class, args);
    }
}
