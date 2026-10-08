package ru.sovmestim.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameNormalizerTest {

    @Test
    void normalizesCaseWhitespaceAndPunctuation() {
        assertThat(NameNormalizer.normalize("  Аспирин   Кардио ")).isEqualTo("аспирин кардио");
        assertThat(NameNormalizer.normalize("ацетил-салициловая, кислота"))
                .isEqualTo("ацетил салициловая кислота");
    }

    @Test
    void foldsCyrillicYoAndHardSign() {
        assertThat(NameNormalizer.normalize("Артёма")).isEqualTo("артема");
        assertThat(NameNormalizer.normalize("съезд")).isEqualTo("сьезд");
    }

    @Test
    void matchesIgnoresEverythingButLettersAndDigits() {
        assertThat(NameNormalizer.matches("Варфарин", "варфарин")).isTrue();
        assertThat(NameNormalizer.matches("Ибупрофен", "ибупрофен ")).isTrue();
        assertThat(NameNormalizer.matches("Варфарин", "Аспирин")).isFalse();
    }

    @Test
    void emptyInputNeverMatches() {
        assertThat(NameNormalizer.normalize(null)).isEmpty();
        assertThat(NameNormalizer.matches("", "")).isFalse();
    }
}
