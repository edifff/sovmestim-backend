package ru.sovmestim.advice.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
 */
@Component
public class AllergyRuleEvaluator {

    private final AllergyRuleSet rules;

    public AllergyRuleEvaluator(AllergyRuleSet rules) {
        this.rules = rules;
    }

    public List<AdviceFinding> evaluate(List<PatientAllergy> allergies, List<SubstanceRef> substances) {
        List<AdviceFinding> findings = new ArrayList<>();
        if (allergies.isEmpty() || substances.isEmpty()) {
            return findings;
        }
        Set<String> seen = new HashSet<>();
        for (PatientAllergy allergy : allergies) {
            for (SubstanceRef substance : substances) {
                String reason = matchReason(allergy, substance);
                if (reason == null) {
                    continue;
                }
                if (!seen.add(allergy.name() + "|" + substance.name())) {
                    continue;
                }
                AdviceLevel level = rules.levelForSeverity(allergy.severity());
                findings.add(new AdviceFinding(
                        AdviceKind.DRUG_ALLERGY,
                        level,
                        "Аллергия: " + allergy.name(),
                        reason,
                        List.of(substance.name()),
                        List.of(new AdviceSourceRef("own-rule", "allergy_rules", null, "версия " + rules.version()))));
            }
        }
        return findings;
    }

    private String matchReason(PatientAllergy allergy, SubstanceRef substance) {
        if (NameNormalizer.matches(allergy.name(), substance.name())) {
            return "Действующее вещество совпадает с указанным аллергеном.";
        }
        if (allergy.code() != null
                && allergy.code().length() >= 3
                && substance.atcCode() != null
                && substance.atcCode().toUpperCase(Locale.ROOT)
                        .startsWith(allergy.code().toUpperCase(Locale.ROOT))) {
            return "Вещество входит в ту же группу АТС (" + allergy.code() + ").";
        }
        for (AllergyRuleSet.CrossReactivity group : rules.crossReactivity()) {
            if (group.matchesAllergen(allergy.name(), allergy.code())
                    && group.containsSubstance(substance.name(), substance.atcCode())) {
                return "Возможна перекрёстная реактивность в группе «" + group.name() + "».";
            }
        }
        return null;
    }
}
