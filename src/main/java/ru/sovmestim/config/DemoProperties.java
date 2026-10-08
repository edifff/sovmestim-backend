package ru.sovmestim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo-mode switch. When enabled the application seeds the recorded RLS/demo catalog so a
 * presentation works without any paid RLS calls.
 */
@ConfigurationProperties(prefix = "sovmestim.demo")
public record DemoProperties(boolean seed) {}
