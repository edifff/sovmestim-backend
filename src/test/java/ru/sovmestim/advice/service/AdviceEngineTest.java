package ru.sovmestim.advice.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.sovmestim.advice.model.AdviceFinding;
import ru.sovmestim.advice.model.AdviceKind;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.model.AdviceStatus;
import ru.sovmestim.advice.model.DrugRef;
import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.PatientSnapshot;
import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.advice.rules.AllergyRuleEvaluator;
import ru.sovmestim.advice.rules.AllergyRuleLoader;
import ru.sovmestim.advice.rules.AllergyRuleSet;
import ru.sovmestim.advice.rules.ClassMappingLoader;
import ru.sovmestim.advice.rules.RlsClassMapping;
import ru.sovmestim.advice.source.InteractionSource;

/**
 * Tests for the advice engine's classification of drug, allergy, and data findings.
 */
class AdviceEngineTest {

    private static final String CATALOG_VERSION = "test-catalog";
    private static final String WARFARIN = "варфарин";
    private static final String WARFARIN_ATC = "B01AA";
    private static final String AMOXICILLIN = "амоксициллин";

    private RlsClassMapping mapping;
    private AllergyRuleSet rules;
    private AllergyRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        mapping = ClassMappingLoader.load(new ClassPathResource("rules/rls_class_mapping.yaml"));
        rules = AllergyRuleLoader.load(new ClassPathResource("rules/allergy_rules.yaml"));
        evaluator = new AllergyRuleEvaluator(rules);
    }

    @Test
    void mapsInteractionClassToDangerLevel() {
        SubstanceRef warfarin = substance(WARFARIN, WARFARIN_ATC);
        SubstanceRef aspirin = substance("ацетилсалициловая кислота", "B01AC");
        AdviceEngine engine = engine(List.of(interaction(warfarin, aspirin, "pharm", "synergism", "increase_toxicity", null)));

        AdviceResult result = engine.check(
                new PatientSnapshot(UUID.randomUUID(), List.of(warfarin), List.of(), List.of()),
                new DrugRef(null, "Аспирин", List.of(aspirin)),
                CATALOG_VERSION);

        Assertions.assertThat(result.status()).isEqualTo(AdviceStatus.INTERACTION_FOUND);
        Assertions.assertThat(result.level()).isEqualTo(AdviceLevel.AVOID);
        Assertions.assertThat(result.findings()).extracting(AdviceFinding::kind).contains(AdviceKind.DRUG_DRUG);
    }

    @Test
    void explicitLevelFromSourceWins() {
        SubstanceRef a = substance("метформин", "A10B");
        SubstanceRef b = substance("йогексол", "V08AB");
        AdviceEngine engine = engine(List.of(interaction(a, b, null, null, null, AdviceLevel.FORBIDDEN)));

        AdviceResult result = engine.check(
                new PatientSnapshot(UUID.randomUUID(), List.of(a), List.of(), List.of()),
                new DrugRef(null, "Йогексол", List.of(b)),
                CATALOG_VERSION);

        Assertions.assertThat(result.level()).isEqualTo(AdviceLevel.FORBIDDEN);
    }

    @Test
    void emptySourceResponseIsNotSafe() {
        SubstanceRef current = substance(WARFARIN, WARFARIN_ATC);
        SubstanceRef drug = substance("омепразол", "A02BC");
        AdviceEngine engine = engine(List.of());

        AdviceResult result = engine.check(
                new PatientSnapshot(UUID.randomUUID(), List.of(current), List.of(), List.of()),
                new DrugRef(null, "Омепразол", List.of(drug)),
                CATALOG_VERSION);

        Assertions.assertThat(result.status()).isEqualTo(AdviceStatus.NO_INTERACTIONS_REPORTED);
        Assertions.assertThat(result.level()).isNull();
    }

    @Test
    void unresolvedDrugIsInsufficientData() {
        AdviceEngine engine = engine(List.of());
        AdviceResult result = engine.check(
                PatientSnapshot.empty(UUID.randomUUID()), new DrugRef(null, "неизвестное", List.of()), CATALOG_VERSION);

        Assertions.assertThat(result.status()).isEqualTo(AdviceStatus.INSUFFICIENT_DATA);
    }

    @Test
    void duplicateSubstanceIsReported() {
        SubstanceRef warfarin = substance(WARFARIN, WARFARIN_ATC);
        AdviceEngine engine = engine(List.of());

        AdviceResult result = engine.check(
                new PatientSnapshot(UUID.randomUUID(), List.of(warfarin), List.of(), List.of()),
                new DrugRef(null, "Варфарин", List.of(substance(WARFARIN, WARFARIN_ATC))),
                CATALOG_VERSION);

        Assertions.assertThat(result.status()).isEqualTo(AdviceStatus.INTERACTION_FOUND);
        Assertions.assertThat(result.findings()).extracting(AdviceFinding::kind).contains(AdviceKind.DUPLICATE_SUBSTANCE);
    }

    @Test
    void allergyFindingIsIncluded() {
        SubstanceRef drug = substance(AMOXICILLIN, "J01C");
        AdviceEngine engine = engine(List.of());

        AdviceResult result = engine.check(
                new PatientSnapshot(
                        UUID.randomUUID(), List.of(), List.of(new PatientAllergy(AMOXICILLIN, null, "тяжелая")), List.of()),
                new DrugRef(null, "Амоксициллин", List.of(drug)),
                CATALOG_VERSION);

        Assertions.assertThat(result.status()).isEqualTo(AdviceStatus.INTERACTION_FOUND);
        Assertions.assertThat(result.level()).isEqualTo(AdviceLevel.FORBIDDEN);
        Assertions.assertThat(result.findings()).extracting(AdviceFinding::kind).contains(AdviceKind.DRUG_ALLERGY);
    }

    private AdviceEngine engine(List<SubstanceInteraction> interactions) {
        InteractionSource source = new InteractionSource() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public List<SubstanceInteraction> check(Collection<SubstanceRef> substances) {
                return interactions;
            }
        };
        return new AdviceEngine(List.of(source), mapping, evaluator, rules);
    }

    private static SubstanceInteraction interaction(
            SubstanceRef first, SubstanceRef second, String clazz, String subclass, String direction, AdviceLevel level) {
        return new SubstanceInteraction(first, second, clazz, subclass, direction, "desc", level, List.of());
    }

    private static SubstanceRef substance(String name, String atc) {
        return new SubstanceRef(UUID.randomUUID(), name, atc, null, null);
    }
}
