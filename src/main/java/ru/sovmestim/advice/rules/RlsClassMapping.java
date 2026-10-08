package ru.sovmestim.advice.rules;

import java.util.Comparator;
import java.util.List;
import ru.sovmestim.advice.model.AdviceLevel;

/**
 * RLS {@code class}/{@code subclass}/{@code direction} to danger level mapping. This is the small,
 * expert-signed table from the roadmap (tens of rows, not thousands of pairs).
 */
public record RlsClassMapping(String version, AdviceLevel defaultLevel, List<Entry> entries) {

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

    public record Resolution(AdviceLevel level, String explanation) {}

    public Resolution resolve(String clazz, String subclass, String direction) {
        return entries.stream()
                .filter(entry -> entry.matches(clazz, subclass, direction))
                .max(Comparator.comparingInt(Entry::specificity))
                .map(entry -> new Resolution(entry.level(), entry.explanation()))
                .orElseGet(() -> new Resolution(defaultLevel, ""));
    }
}
