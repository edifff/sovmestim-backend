package ru.sovmestim.advice.rules;

import java.util.List;
import java.util.Map;

import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Own rules for drug-allergy matching, including cross-reactivity groups.
 *
 * @param version version of the rules file
 * @param defaultLevel level used when no severity rule applies
 * @param severityLevels normalized severity name to advice level
 * @param crossReactivity cross-reactivity groups of allergens and substances
 */
public record AllergyRuleSet(
        String version,
        AdviceLevel defaultLevel,
        Map<String, AdviceLevel> severityLevels,
        List<CrossReactivity> crossReactivity) {

    /**
     * Resolves the advice level for an allergy severity.
     *
     * @param severity severity name as entered by the patient, may be {@code null}
     * @return level for the severity, or the default level when unknown
     */
    public AdviceLevel levelForSeverity(String severity) {
        if (severity == null) {
            return defaultLevel;
        }
        return severityLevels.getOrDefault(NameNormalizer.normalize(severity), defaultLevel);
    }
}
