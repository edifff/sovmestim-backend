package ru.sovmestim.advice.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of one advice check. {@code level} is {@code null} when no finding was produced.
 */
public record AdviceResult(
        AdviceStatus status,
        AdviceLevel level,
        List<AdviceFinding> findings,
        int checkedSubstances,
        String rulesVersion,
        String mappingVersion,
        String catalogVersion,
        Instant checkedAt,
        List<String> notes) {}
