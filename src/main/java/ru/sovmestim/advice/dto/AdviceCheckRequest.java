package ru.sovmestim.advice.dto;

import java.util.List;
import java.util.UUID;

/**
 * One of {@code medicineId}, {@code drugName} or {@code substanceIds} must be present. An
 * unresolved name yields a safe {@code INSUFFICIENT_DATA} answer instead of an error.
 */
public record AdviceCheckRequest(String drugName, UUID medicineId, List<UUID> substanceIds) {}
