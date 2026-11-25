package ru.sovmestim.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Public REST entry point of the Sovmestim backend. It validates access tokens, serves the OpenAPI
 * contract and orchestrates the domain services over gRPC.
 */
@SpringBootApplication(scanBasePackages = "ru.sovmestim")
@ConfigurationPropertiesScan
public class GatewayApplication {

    private GatewayApplication() {
    }

    /**
     * Starts the gateway.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
