package ru.sovmestim.advice.dto;

import java.util.List;
import java.util.UUID;

/**
 * One of {@code medicineId}, {@code drugName} or {@code substanceIds} must be present. An
 * unresolved name yields a safe {@code INSUFFICIENT_DATA} answer instead of an error.
 *
 * @param drugName free-text drug name to resolve, may be {@code null}
 * @param medicineId catalog medicine id, may be {@code null}
 * @param substanceIds explicit substance ids, may be {@code null}
 */
public record AdviceCheckRequest(String drugName, UUID medicineId, List<UUID> substanceIds) { }
