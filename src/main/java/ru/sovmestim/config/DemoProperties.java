package ru.sovmestim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo-mode switch. When enabled the application seeds the recorded RLS/demo catalog so a
 * presentation works without any paid RLS calls.
 *
 * @param seed whether the demo catalog should be seeded on startup
 */
@ConfigurationProperties(prefix = "sovmestim.demo")
public record DemoProperties(boolean seed) { }
