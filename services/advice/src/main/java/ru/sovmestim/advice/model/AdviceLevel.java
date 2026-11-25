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

    /**
     * Returns the more severe of two levels, treating {@code null} as the less severe one.
     *
     * @param a first level, may be {@code null}
     * @param b second level, may be {@code null}
     * @return the more severe non-null level, or {@code null} when both are {@code null}
     */
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
