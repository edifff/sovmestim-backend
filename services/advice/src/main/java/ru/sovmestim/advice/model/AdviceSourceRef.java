package ru.sovmestim.advice.model;

import java.time.LocalDate;

/**
 * Reference to the source of an interaction fact (official / alternative RLS source, or own rule).
 *
 * @param kind source kind, e.g. {@code official}, {@code alternative} or {@code own-rule}
 * @param name human-readable source name
 * @param date publication date of the source, may be {@code null}
 * @param note additional source note, may be {@code null}
 */
public record AdviceSourceRef(String kind, String name, LocalDate date, String note) { }
