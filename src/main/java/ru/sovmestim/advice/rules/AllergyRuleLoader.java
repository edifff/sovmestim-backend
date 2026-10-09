package ru.sovmestim.advice.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.Resource;

import ru.sovmestim.advice.model.AdviceLevel;
import ru.sovmestim.common.util.NameNormalizer;

/**
 * Loads {@code allergy_rules.yaml}.
 */
public final class AllergyRuleLoader {

    private AllergyRuleLoader() {
    }

    /**
     * Parses the allergy rules file into an immutable rule set.
     *
     * @param resource YAML resource to read
     * @return parsed allergy rule set
     */
    @SuppressWarnings("unchecked")
    public static AllergyRuleSet load(Resource resource) {
        Map<String, Object> root = ClassMappingLoader.readYaml(resource);
        String version = ClassMappingLoader.str(root.get("version"));
        AdviceLevel defaultLevel = parseLevel(ClassMappingLoader.str(root.get("default_level")), AdviceLevel.CAUTION);

        Map<String, AdviceLevel> severityLevels = new LinkedHashMap<>();
        Object severities = root.get("severity_levels");
        if (severities instanceof Map<?, ?> map) {
            map.forEach((key, value) -> severityLevels.put(
                    NameNormalizer.normalize(key.toString()),
                    parseLevel(value == null ? null : value.toString(), defaultLevel)));
        }

        List<Map<String, Object>> groups =
                (List<Map<String, Object>>) root.getOrDefault("cross_reactivity", List.of());
        List<AllergyRuleSet.CrossReactivity> crossReactivity = groups.stream()
                .map(group -> new AllergyRuleSet.CrossReactivity(
                        ClassMappingLoader.str(group.get("name")),
                        ClassMappingLoader.str(group.get("code")),
                        stringList(group.get("atc_prefixes")),
                        stringList(group.get("substances"))))
                .toList();

        return new AllergyRuleSet(version, defaultLevel, severityLevels, crossReactivity);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private static AdviceLevel parseLevel(String text, AdviceLevel fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return AdviceLevel.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
