package ru.sovmestim.advice.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, List<IndexedGroup>> groupsByName;
    private final Map<String, List<IndexedGroup>> groupsByCode;

    /**
     * Creates the evaluator over the configured allergy rules.
     *
     * <p>The cross-reactivity groups are indexed once here: the rule set is immutable and loaded at
     * startup, so rebuilding the index on every evaluation would only repeat the same work.
     *
     * @param rules allergy rule set loaded from configuration
     */
    public AllergyRuleEvaluator(AllergyRuleSet rules) {
        this.rules = rules;
        this.groupsByName = new HashMap<>();
        this.groupsByCode = new HashMap<>();
        indexGroups(groupsByName, groupsByCode);
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
        for (CrossReactivity group : rules.crossReactivity()) {
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
}
