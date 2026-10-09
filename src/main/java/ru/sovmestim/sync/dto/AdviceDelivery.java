package ru.sovmestim.sync.dto;

import java.time.Instant;

import ru.sovmestim.advice.model.AdviceResult;

/**
 * A check result produced by the server (roadmap S4/S5). {@code result} carries the danger level,
 * findings, sources and rule/catalog versions.
 *
 * @param adviceId id of the advice record
 * @param drugName drug the advice was produced for
 * @param createdAt time the advice was produced
 * @param result check result with danger level, findings and sources
 */
public record AdviceDelivery(String adviceId, String drugName, Instant createdAt, AdviceResult result) { }
