package ru.sovmestim.advice.rules;

import java.util.Locale;

import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * A drug substance with its name and ATC code pre-normalized for repeated comparisons.
 *
 * @param ref the original substance reference
 * @param normalizedName NFKC-lowercased substance name
 * @param upperAtc upper-cased ATC code, may be {@code null}
 */
record IndexedSubstance(SubstanceRef ref, String normalizedName, String upperAtc) {

    IndexedSubstance(SubstanceRef ref) {
        this(ref, NameNormalizer.normalize(ref.name()), ref.atcCode() != null
                ? ref.atcCode().toUpperCase(Locale.ROOT)
                : null);
    }

    boolean hasName() {
        return !normalizedName.isEmpty();
    }
}
