package ru.sovmestim.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes drug / substance / allergen names the way the catalog search expects.
 *
 * <p>Rules: Unicode NFKC, lower-case, Cyrillic/Latin homoglyph unification is intentionally avoided
 * (a valid drug name may be Latin), punctuation collapsed to single spaces, whitespace trimmed.
 *
 * <p>The regexes are compiled once: this class sits in the inner loops of the advice engine, where
 * per-call pattern compilation would dominate the cost.
 */
public final class NameNormalizer {

    /** Single space used when collapsing punctuation runs between name parts. */
    private static final String SPACE = " ";

    /** Run of characters that are neither letters nor digits, collapsed into a single space. */
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");

    /** Any run of whitespace collapsed into a single space. */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private NameNormalizer() {
    }

    /**
     * Normalizes a raw name to the canonical catalog form.
     *
     * @param raw the raw name, may be {@code null}
     * @return the normalized name, empty for {@code null} or blank input
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        value = value.toLowerCase(Locale.ROOT);
        value = value.replace('ё', 'е');
        value = value.replace('ъ', 'ь');
        value = NON_ALNUM.matcher(value).replaceAll(SPACE);
        return WHITESPACE.matcher(value.trim()).replaceAll(SPACE);
    }

    /**
     * Tells whether two names are equal after normalization.
     *
     * @param a the first name
     * @param b the second name
     * @return {@code true} when both names match and are not empty after normalization
     */
    public static boolean matches(String a, String b) {
        String normalized = normalize(a);
        return !normalized.isEmpty() && normalized.equals(normalize(b));
    }
}
