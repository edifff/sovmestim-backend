package ru.sovmestim.advice.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import ru.sovmestim.advice.model.AdviceFinding;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.SubstanceRef;

class AllergyRuleEvaluatorTest {

    private static AllergyRuleEvaluator evaluator;

    @BeforeAll
    static void setUp() {
        AllergyRuleSet rules = AllergyRuleLoader.load(new ClassPathResource("rules/allergy_rules.yaml"));
        evaluator = new AllergyRuleEvaluator(rules);
    }

    @Test
    void matchesSameSubstanceNameAndUsesSeverityLevel() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy("амоксициллин", null, "тяжелая")),
                List.of(substance("амоксициллин", "J01C")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).level()).isEqualTo(AdviceLevel.FORBIDDEN);
    }

    @Test
    void matchesCrossReactivityGroupByName() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy("Пенициллины", null, null)),
                List.of(substance("ампициллин", "J01C")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).level()).isEqualTo(AdviceLevel.CAUTION);
        assertThat(findings.get(0).text()).contains("перекрёстная");
    }

    @Test
    void matchesByAtcPrefix() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy("НПВС", "M01A", "средняя")),
                List.of(substance("ибупрофен", "M01AE")));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).level()).isEqualTo(AdviceLevel.AVOID);
    }

    @Test
    void returnsNothingForUnrelatedAllergy() {
        List<AdviceFinding> findings = evaluator.evaluate(
                List.of(new PatientAllergy("Пенициллины", "J01C", null)),
                List.of(substance("варфарин", "B01AA")));

        assertThat(findings).isEmpty();
    }

    private static SubstanceRef substance(String name, String atc) {
        return new SubstanceRef(UUID.randomUUID(), name, atc, null, null);
    }
}
