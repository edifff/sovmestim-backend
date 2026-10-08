package ru.sovmestim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import ru.sovmestim.advice.rules.AllergyRuleLoader;
import ru.sovmestim.advice.rules.AllergyRuleSet;
import ru.sovmestim.advice.rules.ClassMappingLoader;
import ru.sovmestim.advice.rules.RlsClassMapping;

/**
 * Loads the expert-signed rule files from Git (class mapping and own allergy rules).
 */
@Configuration
public class RulesConfig {

    @Bean
    RlsClassMapping rlsClassMapping(RulesProperties properties, ResourceLoader resourceLoader) {
        return ClassMappingLoader.load(resourceLoader.getResource(properties.classMapping()));
    }

    @Bean
    AllergyRuleSet allergyRuleSet(RulesProperties properties, ResourceLoader resourceLoader) {
        return AllergyRuleLoader.load(resourceLoader.getResource(properties.allergyRules()));
    }
}
