package ru.sovmestim.advice.rules;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Own rules for drug-allergy matching, including cross-reactivity groups.
 */
public record AllergyRuleSet(
        String version,
        AdviceLevel defaultLevel,
        Map<String, AdviceLevel> severityLevels,
        List<CrossReactivity> crossReactivity) {

    /**
     * @param atcPrefixes ATC code prefixes that mark membership in the group
     * @param substances  normalized substance names belonging to the group
     */
    public record CrossReactivity(String name, String code, List<String> atcPrefixes, List<String> substances) {

        public boolean matchesAllergen(String allergyName, String allergyCode) {
            if (allergyCode != null && code != null && allergyCode.equalsIgnoreCase(code)) {
                return true;
            }
            return NameNormalizer.matches(name, allergyName);
        }

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

    public AdviceLevel levelForSeverity(String severity) {
        if (severity == null) {
            return defaultLevel;
        }
        return severityLevels.getOrDefault(NameNormalizer.normalize(severity), defaultLevel);
    }
}
