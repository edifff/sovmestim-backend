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
public record RlsClassMapping(String version, AdviceLevel defaultLevel, List<Entry> entries) {

    /**
     * Resolves the danger level for a drug classification triple.
     *
     * @param clazz RLS pharmacological class
     * @param subclass RLS pharmacological subclass
     * @param direction RLS direction of use
     * @return most specific matching entry, or the default level with an empty explanation
     */
    public Resolution resolve(String clazz, String subclass, String direction) {
        return entries.stream()
                .filter(entry -> entry.matches(clazz, subclass, direction))
                .max(Comparator.comparingInt(Entry::specificity))
                .map(entry -> new Resolution(entry.level(), entry.explanation()))
                .orElseGet(() -> new Resolution(defaultLevel, ""));
    }

    /**
     * One expert-signed mapping row with optional wildcard fields.
     *
     * @param clazz pharmacological class pattern, {@code null} matches any
     * @param subclass pharmacological subclass pattern, {@code null} matches any
     * @param direction direction pattern, {@code null} matches any
     * @param level danger level assigned by this row
     * @param explanation expert explanation shown to the user
     */
    public record Entry(
            String clazz, String subclass, String direction, AdviceLevel level, String explanation) {

        boolean matches(String actualClass, String actualSubclass, String actualDirection) {
            return wildcardEquals(clazz, actualClass)
                    && wildcardEquals(subclass, actualSubclass)
                    && wildcardEquals(direction, actualDirection);
        }

        int specificity() {
            return (clazz != null ? 1 : 0) + (subclass != null ? 1 : 0) + (direction != null ? 1 : 0);
        }

        private static boolean wildcardEquals(String pattern, String actual) {
            return pattern == null || (actual != null && pattern.equalsIgnoreCase(actual));
        }
    }

    /**
     * Resolved danger level together with its explanation.
     *
     * @param level resolved danger level
     * @param explanation expert explanation, may be empty
     */
    public record Resolution(AdviceLevel level, String explanation) { }
}
