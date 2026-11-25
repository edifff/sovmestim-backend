package ru.sovmestim.common.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NameNormalizer}.
 */
class NameNormalizerTest {

    private static final String WARFARIN = "Варфарин";

    @Test
    void normalizesCaseWhitespaceAndPunctuation() {
        Assertions.assertThat(NameNormalizer.normalize("  Аспирин   Кардио ")).isEqualTo("аспирин кардио");
        Assertions.assertThat(NameNormalizer.normalize("ацетил-салициловая, кислота"))
                .isEqualTo("ацетил салициловая кислота");
    }

    @Test
    void foldsCyrillicYoAndHardSign() {
        Assertions.assertThat(NameNormalizer.normalize("Артёма")).isEqualTo("артема");
        Assertions.assertThat(NameNormalizer.normalize("съезд")).isEqualTo("сьезд");
    }

    @Test
    void matchesIgnoresEverythingButLettersAndDigits() {
        Assertions.assertThat(NameNormalizer.matches(WARFARIN, "варфарин")).isTrue();
        Assertions.assertThat(NameNormalizer.matches("Ибупрофен", "ибупрофен ")).isTrue();
        Assertions.assertThat(NameNormalizer.matches(WARFARIN, "Аспирин")).isFalse();
    }

    @Test
    void emptyInputNeverMatches() {
        Assertions.assertThat(NameNormalizer.normalize(null)).isEmpty();
        Assertions.assertThat(NameNormalizer.matches("", "")).isFalse();
    }
}
