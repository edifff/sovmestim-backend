package ru.sovmestim.advice.rules;

import java.util.Comparator;
import java.util.List;

import ru.sovmestim.advice.model.AdviceLevel;

/**
 * RLS {@code class}/{@code subclass}/{@code direction} to danger level mapping. This is the small,
 * expert-signed table from the roadmap (tens of rows, not thousands of pairs).
 *
 * @param version version of the mapping table
 * @param defaultLevel level applied when no entry matches
 * @param entries mapping entries ordered by increasing specificity
 */
public record RlsClassMapping(String version, AdviceLevel defaultLevel, List<ClassMappingEntry> entries) {

    /**
     * Resolves the danger level for a drug classification triple.
     *
     * @param clazz RLS pharmacological class
     * @param subclass RLS pharmacological subclass
     * @param direction RLS direction of use
     * @return most specific matching entry, or the default level with an empty explanation
     */
    public ClassMappingResolution resolve(String clazz, String subclass, String direction) {
        return entries.stream()
                .filter(entry -> entry.matches(clazz, subclass, direction))
                .max(Comparator.comparingInt(ClassMappingEntry::specificity))
                .map(entry -> new ClassMappingResolution(entry.level(), entry.explanation()))
                .orElseGet(() -> new ClassMappingResolution(defaultLevel, ""));
    }
}
