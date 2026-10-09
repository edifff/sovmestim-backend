package ru.sovmestim.advice.rules;

import java.util.List;
import java.util.Locale;

import ru.sovmestim.common.util.NameNormalizer;

/**
 * A group of substances that may cross-react with a given allergen.
 *
 * @param name human-readable group name
 * @param code allergen group code matched against the allergy code, may be {@code null}
 * @param atcPrefixes ATC code prefixes that mark membership in the group
 * @param substances normalized substance names belonging to the group
 */
public record CrossReactivity(String name, String code, List<String> atcPrefixes, List<String> substances) {

    /**
     * Checks whether the group matches the given allergen.
     *
     * @param allergyName allergen substance name, may be {@code null}
     * @param allergyCode allergen ATC or group code, may be {@code null}
     * @return {@code true} when the allergen belongs to this group
     */
    public boolean matchesAllergen(String allergyName, String allergyCode) {
        if (allergyCode != null && code != null && allergyCode.equalsIgnoreCase(code)) {
            return true;
        }
        return NameNormalizer.matches(name, allergyName);
    }

    /**
     * Checks whether the given substance belongs to this cross-reactivity group.
     *
     * @param substanceName substance name to test, may be {@code null}
     * @param substanceAtc substance ATC code to test, may be {@code null}
     * @return {@code true} when the substance is part of this group
     */
    public boolean containsSubstance(String substanceName, String substanceAtc) {
        if (substanceName != null
                && substances.stream().anyMatch(candidate -> NameNormalizer.matches(candidate, substanceName))) {
            return true;
        }
        return substanceAtc != null
                && atcPrefixes.stream().anyMatch(prefix -> substanceAtc.toUpperCase(Locale.ROOT)
                        .startsWith(prefix.toUpperCase(Locale.ROOT)));
    }
}
