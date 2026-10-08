package ru.sovmestim.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalizes drug / substance / allergen names the way the catalog search expects.
 *
 * <p>Rules: Unicode NFKC, lower-case, Cyrillic/Latin homoglyph unification is intentionally avoided
 * (a valid drug name may be Latin), punctuation collapsed to single spaces, whitespace trimmed.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        value = value.toLowerCase(Locale.ROOT);
        value = value.replace('ё', 'е');
        value = value.replace('ъ', 'ь');
        value = value.replaceAll("[^\\p{L}\\p{N}]+", " ");
        return value.trim().replaceAll("\\s+", " ");
    }

    /**
     * True when two names are equal after normalization.
     */
    public static boolean matches(String a, String b) {
        return !normalize(a).isEmpty() && normalize(a).equals(normalize(b));
    }
}
