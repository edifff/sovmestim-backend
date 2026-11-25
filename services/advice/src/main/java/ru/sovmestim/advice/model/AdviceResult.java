package ru.sovmestim.advice.model;

import java.time.Instant;
import java.util.List;

/**
 * Result of one advice check. {@code level} is {@code null} when no finding was produced.
 *
 * @param status overall status of the check
 * @param level highest severity among the findings, may be {@code null}
 * @param findings reported findings
 * @param checkedSubstances number of distinct substances checked
 * @param rulesVersion version of the own rules
 * @param mappingVersion version of the class-mapping table
 * @param catalogVersion version of the catalog used for resolution
 * @param checkedAt instant the check was computed
 * @param notes informational notes shown next to the result
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
        List<String> notes) { }
