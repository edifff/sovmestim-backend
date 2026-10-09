package ru.sovmestim.advice.rules;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.sovmestim.advice.model.AdviceLevel;

/**
 * Tests for loading and resolving the RLS class mapping.
 */
class ClassMappingLoaderTest {

    private static final String PHARM_CLASS = "pharm";
    private static final String SYNERGISM_SUBCLASS = "synergism";

    private static RlsClassMapping mapping;

    @BeforeAll
    static void load() {
        mapping = ClassMappingLoader.load(new ClassPathResource("rules/rls_class_mapping.yaml"));
    }

    @Test
    void readsVersionAndDefaultLevel() {
        Assertions.assertThat(mapping.version()).isEqualTo("1.0.0");
        Assertions.assertThat(mapping.defaultLevel()).isEqualTo(AdviceLevel.INFO);
    }

    @Test
    void resolvesExactEntry() {
        Assertions.assertThat(mapping.resolve(PHARM_CLASS, "metabolism", "increase_exposure").level())
                .isEqualTo(AdviceLevel.AVOID);
        Assertions.assertThat(mapping.resolve("contra", null, "contrast_nephropathy").level())
                .isEqualTo(AdviceLevel.FORBIDDEN);
    }

    @Test
    void prefersMostSpecificEntry() {
        // "pharm" alone is a CAUTION fallback, but the precise subclass/direction wins.
        Assertions.assertThat(mapping.resolve(PHARM_CLASS, SYNERGISM_SUBCLASS, "increase_toxicity").level())
                .isEqualTo(AdviceLevel.AVOID);
        Assertions.assertThat(mapping.resolve(PHARM_CLASS, SYNERGISM_SUBCLASS, "unknown").level()).isEqualTo(AdviceLevel.CAUTION);
    }

    @Test
    void fallsBackToDefaultForUnknownClass() {
        Assertions.assertThat(mapping.resolve("totally-unknown", null, null).level()).isEqualTo(AdviceLevel.INFO);
    }
}
