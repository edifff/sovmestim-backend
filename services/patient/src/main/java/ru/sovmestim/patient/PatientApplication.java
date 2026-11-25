package ru.sovmestim.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Patient service entry point: allergies and chronic conditions.
 */
@SpringBootApplication(scanBasePackages = "ru.sovmestim")
public class PatientApplication {

    private PatientApplication() {
    }

    /**
     * Starts the patient service.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(PatientApplication.class, args);
    }
}
