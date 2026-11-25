package ru.sovmestim.advice.model;

/**
 * Outcome of a check. An empty source response becomes {@link #NO_INTERACTIONS_REPORTED}, which is
 * explicitly not the same as "safe".
 */
public enum AdviceStatus {
    INTERACTION_FOUND,
    NO_INTERACTIONS_REPORTED,
    INSUFFICIENT_DATA
}
