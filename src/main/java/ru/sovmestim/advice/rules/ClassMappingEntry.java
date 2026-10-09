package ru.sovmestim.advice.rules;

import ru.sovmestim.advice.model.AdviceLevel;

/**
 * One expert-signed mapping row with optional wildcard fields.
 *
 * @param clazz pharmacological class pattern, {@code null} matches any
 * @param subclass pharmacological subclass pattern, {@code null} matches any
 * @param direction direction pattern, {@code null} matches any
 * @param level danger level assigned by this row
 * @param explanation expert explanation shown to the user
 */
public record ClassMappingEntry(
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
