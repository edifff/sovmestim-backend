package ru.sovmestim.advice.model;

/**
 * Danger level of an advice finding. Ordered from least to most severe. The value {@code SAFE} does
 * not exist by design: "no data" must never be shown as "safe" (architecture §2, §9).
 */
public enum AdviceLevel {
    INFO,
    CAUTION,
    AVOID,
    FORBIDDEN;

    public static AdviceLevel max(AdviceLevel a, AdviceLevel b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
