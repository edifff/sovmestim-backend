package ru.sovmestim.advice.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ru.sovmestim.advice.model.AdviceLevel;

class ClassMappingLoaderTest {

    private static RlsClassMapping mapping;

    @BeforeAll
    static void load() {
        mapping = ClassMappingLoader.load(new ClassPathResource("rules/rls_class_mapping.yaml"));
    }

    @Test
    void readsVersionAndDefaultLevel() {
        assertThat(mapping.version()).isEqualTo("1.0.0");
        assertThat(mapping.defaultLevel()).isEqualTo(AdviceLevel.INFO);
    }

    @Test
    void resolvesExactEntry() {
        assertThat(mapping.resolve("pharm", "metabolism", "increase_exposure").level())
                .isEqualTo(AdviceLevel.AVOID);
        assertThat(mapping.resolve("contra", null, "contrast_nephropathy").level())
                .isEqualTo(AdviceLevel.FORBIDDEN);
    }

    @Test
    void prefersMostSpecificEntry() {
        // "pharm" alone is a CAUTION fallback, but the precise subclass/direction wins.
        assertThat(mapping.resolve("pharm", "synergism", "increase_toxicity").level())
                .isEqualTo(AdviceLevel.AVOID);
        assertThat(mapping.resolve("pharm", "synergism", "unknown").level()).isEqualTo(AdviceLevel.CAUTION);
    }

    @Test
    void fallsBackToDefaultForUnknownClass() {
        assertThat(mapping.resolve("totally-unknown", null, null).level()).isEqualTo(AdviceLevel.INFO);
    }
}
