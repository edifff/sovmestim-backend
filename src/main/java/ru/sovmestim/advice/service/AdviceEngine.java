package ru.sovmestim.advice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ru.sovmestim.advice.model.AdviceFinding;
import ru.sovmestim.advice.model.AdviceKind;
import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.advice.model.AdviceResult;
import ru.sovmestim.advice.model.AdviceSourceRef;
import ru.sovmestim.advice.model.AdviceStatus;
import ru.sovmestim.advice.model.DrugRef;
import ru.sovmestim.advice.model.PatientSnapshot;
import ru.sovmestim.advice.model.SubstanceInteraction;
import ru.sovmestim.advice.model.SubstanceRef;
import ru.sovmestim.advice.rules.AllergyRuleEvaluator;
import ru.sovmestim.advice.rules.AllergyRuleSet;
import ru.sovmestim.advice.rules.RlsClassMapping;
import ru.sovmestim.advice.source.InteractionSource;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Pure decision logic (no Spring, no I/O): composes drug-drug interactions from the configured
 * sources, the own allergy rule and a duplicate-substance check into one {@link AdviceResult}.
 * It never invents interactions and never uses the status "safe".
 */
@Service
public class AdviceEngine {

    /** Separator used in the canonical pair key of two substances. */
    private static final String PAIR_SEPARATOR = "|";

    private final List<InteractionSource> sources;
    private final RlsClassMapping classMapping;
    private final AllergyRuleEvaluator allergyEvaluator;
    private final AllergyRuleSet allergyRules;

    /**
     * Creates the engine over the configured interaction sources and own rules.
     *
     * @param sources interaction sources consulted in order
     * @param classMapping class-to-level mapping table
     * @param allergyEvaluator own drug-allergy rule
     * @param allergyRules allergy rule set providing the rules version
     */
    public AdviceEngine(
            List<InteractionSource> sources,
            RlsClassMapping classMapping,
            AllergyRuleEvaluator allergyEvaluator,
            AllergyRuleSet allergyRules) {
        this.sources = sources;
        this.classMapping = classMapping;
        this.allergyEvaluator = allergyEvaluator;
        this.allergyRules = allergyRules;
    }

    /**
     * Runs the full advice check for the given patient and drug.
     *
     * @param patient patient state at check time
     * @param drug drug being checked
     * @param catalogVersion catalog version recorded for provenance
     * @return composed advice result with status, findings and notes
     */
    public AdviceResult check(PatientSnapshot patient, DrugRef drug, String catalogVersion) {
        List<SubstanceRef> distinct = distinct(patient.currentSubstances(), drug.substances());
        List<String> notes = new ArrayList<>();
        List<AdviceFinding> findings = new ArrayList<>();

        findings.addAll(interactionFindings(drug, gather(distinct)));
        findings.addAll(duplicateFindings(patient, drug));
        findings.addAll(allergyEvaluator.evaluate(patient.allergies(), drug.substances()));

        if (!patient.conditions().isEmpty()) {
            notes.add("Проверка «лекарство–заболевание» пока недоступна: для этого источника нет "
                    + "структурированных данных, результат не означает безопасность.");
        }

        AdviceStatus status;
        if (drug.substances().isEmpty()) {
            status = AdviceStatus.INSUFFICIENT_DATA;
            notes.add("Название препарата не распознано. Данных для проверки недостаточно.");
        } else if (!findings.isEmpty()) {
            status = AdviceStatus.INTERACTION_FOUND;
        } else {
            status = AdviceStatus.NO_INTERACTIONS_REPORTED;
            notes.add("RLS не вернул взаимодействий по доступным веществам. Это не значит «безопасно».");
        }

        AdviceLevel level = findings.stream()
                .map(AdviceFinding::level)
                .reduce((a, b) -> AdviceLevel.max(a, b))
                .orElse(null);

        return new AdviceResult(
                status,
                level,
                List.copyOf(findings),
                distinct.size(),
                allergyRules.version(),
                classMapping.version(),
                catalogVersion,
                Instant.now(),
                List.copyOf(notes));
    }

    private List<AdviceFinding> interactionFindings(DrugRef drug, List<SubstanceInteraction> interactions) {
        List<AdviceFinding> findings = new ArrayList<>();
        Set<String> drugSubstances = drug.substances().stream()
                .map(substance -> NameNormalizer.normalize(substance.name()))
                .collect(Collectors.toSet());
        for (SubstanceInteraction interaction : interactions) {
            boolean involvesDrug = drugSubstances.contains(NameNormalizer.normalize(interaction.substance1().name()))
                    || drugSubstances.contains(NameNormalizer.normalize(interaction.substance2().name()));
            if (!involvesDrug) {
                continue;
            }
            findings.add(toFinding(interaction));
        }
        return findings;
    }

    private List<AdviceFinding> duplicateFindings(PatientSnapshot patient, DrugRef drug) {
        // One normalized set of the patient's current substances instead of a nested scan.
        Set<String> currentNames = patient.currentSubstances().stream()
                .map(current -> NameNormalizer.normalize(current.name()))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
        List<AdviceFinding> findings = new ArrayList<>();
        if (currentNames.isEmpty()) {
            return findings;
        }
        for (SubstanceRef substance : drug.substances()) {
            String normalized = NameNormalizer.normalize(substance.name());
            if (currentNames.contains(normalized)) {
                findings.add(new AdviceFinding(
                        AdviceKind.DUPLICATE_SUBSTANCE,
                        AdviceLevel.CAUTION,
                        "Дублирование: " + substance.name(),
                        "Действующее вещество уже присутствует в списке текущих приёмов.",
                        List.of(substance.name()),
                        List.of(new AdviceSourceRef("own-rule", "duplicate_check", null, null))));
            }
        }
        return findings;
    }

    private AdviceFinding toFinding(SubstanceInteraction interaction) {
        AdviceLevel level;
        String explanation;
        List<AdviceSourceRef> sources = interaction.sources();
        if (interaction.explicitLevel() != null) {
            level = interaction.explicitLevel();
            explanation = interaction.description();
        } else {
            RlsClassMapping.Resolution resolution =
                    classMapping.resolve(interaction.clazz(), interaction.subclass(), interaction.direction());
            level = resolution.level();
            explanation = resolution.explanation() != null && !resolution.explanation().isBlank()
                    ? resolution.explanation()
                    : interaction.description();
        }
        return new AdviceFinding(
                AdviceKind.DRUG_DRUG,
                level,
                interaction.substance1().name() + " + " + interaction.substance2().name(),
                explanation,
                List.of(interaction.substance1().name(), interaction.substance2().name()),
                sources);
    }

    private List<SubstanceInteraction> gather(List<SubstanceRef> substances) {
        Map<String, SubstanceInteraction> merged = new LinkedHashMap<>();
        for (InteractionSource source : sources) {
            for (SubstanceInteraction interaction : source.check(substances)) {
                merged.putIfAbsent(pairKey(interaction), interaction);
            }
        }
        return List.copyOf(merged.values());
    }

    private static String pairKey(SubstanceInteraction interaction) {
        String first = NameNormalizer.normalize(interaction.substance1().name());
        String second = NameNormalizer.normalize(interaction.substance2().name());
        return first.compareTo(second) <= 0 ? first + PAIR_SEPARATOR + second : second + PAIR_SEPARATOR + first;
    }

    private static List<SubstanceRef> distinct(List<SubstanceRef> first, List<SubstanceRef> second) {
        Set<String> seen = new LinkedHashSet<>();
        List<SubstanceRef> result = new ArrayList<>();
        for (SubstanceRef substance : concat(first, second)) {
            if (substance.name() == null || NameNormalizer.normalize(substance.name()).isBlank()) {
                continue;
            }
            if (seen.add(NameNormalizer.normalize(substance.name()))) {
                result.add(substance);
            }
        }
        return result;
    }

    private static List<SubstanceRef> concat(List<SubstanceRef> first, List<SubstanceRef> second) {
        List<SubstanceRef> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }
}
