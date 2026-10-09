package ru.sovmestim.advice.dto;

import java.util.UUID;

import ru.sovmestim.advice.model.AdviceResult;

/**
 * Stored advice answer: the audit record id together with the engine result.
 *
 * @param adviceId id of the stored audit record
 * @param result engine result for the check
 */
public record AdviceCheckResponse(UUID adviceId, AdviceResult result) { }
