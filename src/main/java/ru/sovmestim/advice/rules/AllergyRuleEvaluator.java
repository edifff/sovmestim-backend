package ru.sovmestim.advice.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ru.sovmestim.advice.model.AdviceFinding;
import ru.sovmestim.advice.model.AdviceKind;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.AdviceSourceRef;
import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Own drug-allergy rule: a patient allergen (substance name, ATC code or cross-reactivity group)
 * matched against the substances of a drug.
 *
 * <p>Matching is indexed instead of scanning: within one evaluation the cross-reactivity groups are
 * bucketed by allergen name and code, and their substance lists are pre-normalized into sets, so
 * the per-pair matching never re-normalizes. The index lives only inside the call, the rule set
 * stays the single source of truth.
 */
@Component
public class AllergyRuleEvaluator {

    private final AllergyRuleSet rules;

    /**
     * Creates the evaluator over the configured allergy rules.
     *
     * @param rules allergy rule set loaded from configuration
     */
    public AllergyRuleEvaluator(AllergyRuleSet rules) {
        this.rules = rules;
    }

    /**
     * Evaluates the patient's allergies against the substances of the checked drug.
     *
     * @param allergies patient allergies to match
     * @param substances active substances of the drug being checked
     * @return findings for every matched allergy, empty when nothing matches
     */
    public List<AdviceFinding> evaluate(List<PatientAllergy> allergies, List<SubstanceRef> substances) {
        List<AdviceFinding> findings = new ArrayList<>();
        if (allergies.isEmpty() || substances.isEmpty()) {
            return findings;
        }
        Map<String, List<IndexedGroup>> groupsByName = new HashMap<>();
        Map<String, List<IndexedGroup>> groupsByCode = new HashMap<>();
        indexGroups(groupsByName, groupsByCode);

        List<IndexedSubstance> drugSubstances = substances.stream().map(IndexedSubstance::new).toList();
        Set<String> seen = new HashSet<>();
        for (PatientAllergy allergy : allergies) {
            String normalizedAllergen = NameNormalizer.normalize(allergy.name());
            String upperCode = allergy.code() != null ? allergy.code().toUpperCase(Locale.ROOT) : null;
            for (IndexedSubstance substance : drugSubstances) {
                String reason = matchReason(allergy, normalizedAllergen, upperCode, substance,
                        groupsByName, groupsByCode);
                if (reason == null) {
                    continue;
                }
                if (!seen.add(allergy.name() + "|" + substance.ref().name())) {
                    continue;
                }
                AdviceLevel level = rules.levelForSeverity(allergy.severity());
                findings.add(new AdviceFinding(
                        AdviceKind.DRUG_ALLERGY,
                        level,
                        "Аллергия: " + allergy.name(),
                        reason,
                        List.of(substance.ref().name()),
                        List.of(new AdviceSourceRef("own-rule", "allergy_rules", null, "версия " + rules.version()))));
            }
        }
        return findings;
    }

    private void indexGroups(Map<String, List<IndexedGroup>> groupsByName, Map<String, List<IndexedGroup>> groupsByCode) {
        for (AllergyRuleSet.CrossReactivity group : rules.crossReactivity()) {
            IndexedGroup entry = new IndexedGroup(group);
            String normalizedName = NameNormalizer.normalize(group.name());
            if (!normalizedName.isEmpty()) {
                groupsByName.computeIfAbsent(normalizedName, key -> new ArrayList<>()).add(entry);
            }
            if (group.code() != null) {
                groupsByCode.computeIfAbsent(group.code().toUpperCase(Locale.ROOT), key -> new ArrayList<>())
                        .add(entry);
            }
        }
    }

    private static String matchReason(
            PatientAllergy allergy,
            String normalizedAllergen,
            String upperCode,
            IndexedSubstance substance,
            Map<String, List<IndexedGroup>> groupsByName,
            Map<String, List<IndexedGroup>> groupsByCode) {
        if (!normalizedAllergen.isEmpty() && normalizedAllergen.equals(substance.normalizedName())) {
            return "Действующее вещество совпадает с указанным аллергеном.";
        }
        if (upperCode != null
                && upperCode.length() >= 3
                && substance.upperAtc() != null
                && substance.upperAtc().startsWith(upperCode)) {
            return "Вещество входит в ту же группу АТС (" + allergy.code() + ").";
        }
        for (IndexedGroup group : candidateGroups(normalizedAllergen, upperCode, groupsByName, groupsByCode)) {
            if (group.containsSubstance(substance)) {
                return "Возможна перекрёстная реактивность в группе «" + group.name() + "».";
            }
        }
        return null;
    }

    private static List<IndexedGroup> candidateGroups(
            String normalizedAllergen,
            String upperCode,
            Map<String, List<IndexedGroup>> groupsByName,
            Map<String, List<IndexedGroup>> groupsByCode) {
        List<IndexedGroup> candidates = new ArrayList<>();
        if (!normalizedAllergen.isEmpty()) {
            List<IndexedGroup> byName = groupsByName.get(normalizedAllergen);
            if (byName != null) {
                candidates.addAll(byName);
            }
        }
        if (upperCode != null) {
            List<IndexedGroup> byCode = groupsByCode.get(upperCode);
            if (byCode != null) {
                for (IndexedGroup group : byCode) {
                    if (!candidates.contains(group)) {
                        candidates.add(group);
                    }
                }
            }
        }
        return candidates;
    }

    /**
     * A drug substance with its name and ATC code pre-normalized for repeated comparisons.
     *
     * @param ref the original substance reference
     * @param normalizedName NFKC-lowercased substance name
     * @param upperAtc upper-cased ATC code, may be {@code null}
     */
    private record IndexedSubstance(SubstanceRef ref, String normalizedName, String upperAtc) {

        IndexedSubstance(SubstanceRef ref) {
            this(ref, NameNormalizer.normalize(ref.name()), ref.atcCode() != null
                    ? ref.atcCode().toUpperCase(Locale.ROOT)
                    : null);
        }

        boolean hasName() {
            return !normalizedName.isEmpty();
        }
    }

    /**
     * A cross-reactivity group with pre-normalized lookups so that matching never re-normalizes.
     *
     * @param group the original rule group
     * @param substanceNames normalized substance names of the group
     * @param upperAtcPrefixes upper-cased ATC prefixes of the group
     */
    private record IndexedGroup(
            AllergyRuleSet.CrossReactivity group, Set<String> substanceNames, List<String> upperAtcPrefixes) {

        IndexedGroup(AllergyRuleSet.CrossReactivity group) {
            this(
                    group,
                    group.substances().stream()
                            .map(NameNormalizer::normalize)
                            .filter(name -> !name.isEmpty())
                            .collect(Collectors.toSet()),
                    group.atcPrefixes().stream().map(prefix -> prefix.toUpperCase(Locale.ROOT)).toList());
        }

        String name() {
            return group.name();
        }

        boolean containsSubstance(IndexedSubstance substance) {
            if (substance.hasName() && substanceNames.contains(substance.normalizedName())) {
                return true;
            }
            if (substance.upperAtc() == null) {
                return false;
            }
            for (String prefix : upperAtcPrefixes) {
                if (substance.upperAtc().startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
