package ru.sovmestim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rule file locations and demo-mode switches.
 *
 * @param classMapping location of the RLS class mapping file
 * @param allergyRules location of the allergy rule file
 */
@ConfigurationProperties(prefix = "sovmestim.rules")
public record RulesProperties(String classMapping, String allergyRules) {

    /**
     * Creates the properties, falling back to the bundled rule files when not configured.
     */
    public RulesProperties {
        if (classMapping == null || classMapping.isBlank()) {
            classMapping = "classpath:rules/rls_class_mapping.yaml";
        }
        if (allergyRules == null || allergyRules.isBlank()) {
            allergyRules = "classpath:rules/allergy_rules.yaml";
        }
    }
}
