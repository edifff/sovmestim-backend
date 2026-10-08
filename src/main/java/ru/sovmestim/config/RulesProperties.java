package ru.sovmestim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rule file locations and demo-mode switches.
 */
@ConfigurationProperties(prefix = "sovmestim.rules")
public record RulesProperties(String classMapping, String allergyRules) {

    public RulesProperties {
        if (classMapping == null || classMapping.isBlank()) {
            classMapping = "classpath:rules/rls_class_mapping.yaml";
        }
        if (allergyRules == null || allergyRules.isBlank()) {
            allergyRules = "classpath:rules/allergy_rules.yaml";
        }
    }
}
