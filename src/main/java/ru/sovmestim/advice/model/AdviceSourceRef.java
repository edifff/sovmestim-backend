package ru.sovmestim.advice.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Reference to the source of an interaction fact (official / alternative RLS source, or own rule).
 */
public record AdviceSourceRef(String kind, String name, LocalDate date, String note) {}
