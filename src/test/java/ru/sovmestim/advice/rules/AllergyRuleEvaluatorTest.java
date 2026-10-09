package ru.sovmestim.advice.rules;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.sovmestim.advice.model.AdviceFinding;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.SubstanceRef;

/**
 * Tests for the allergy rule evaluator.
 */
class AllergyRuleEvaluatorTest {

    private static final String AMOXICILLIN = "амоксициллин";
    private static final String PENICILLINS = "Пенициллины";
    private static final String PENICILLIN_ATC = "J01C";

    private static AllergyRuleEvaluator evaluator;

    @BeforeAll
    static void setUp() {
        AllergyRuleSet rules = AllergyRuleLoader.load(new ClassPathResource("rules/allergy_rules.yaml"));
        evaluator = new AllergyRuleEvaluator(rules);
    }

    @Test
    void matchesSameSubstanceNameAndUsesSeverityLevel() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy(AMOXICILLIN, null, "тяжелая")),
                List.of(substance(AMOXICILLIN, PENICILLIN_ATC)));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.get(0).level()).isEqualTo(AdviceLevel.FORBIDDEN);
    }

    @Test
    void matchesCrossReactivityGroupByName() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy(PENICILLINS, null, null)),
                List.of(substance("ампициллин", PENICILLIN_ATC)));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.get(0).level()).isEqualTo(AdviceLevel.CAUTION);
        Assertions.assertThat(findings.get(0).text()).contains("перекрёстная");
    }

    @Test
    void matchesByAtcPrefix() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy("НПВС", "M01A", "средняя")),
                List.of(substance("ибупрофен", "M01AE")));

        Assertions.assertThat(findings).hasSize(1);
        Assertions.assertThat(findings.get(0).level()).isEqualTo(AdviceLevel.AVOID);
    }

    @Test
    void returnsNothingForUnrelatedAllergy() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy(PENICILLINS, PENICILLIN_ATC, null)),
                List.of(substance("варфарин", "B01AA")));

        Assertions.assertThat(findings).isEmpty();
    }

    private static SubstanceRef substance(String name, String atc) {
        return new SubstanceRef(UUID.randomUUID(), name, atc, null, null);
    }
}
