package ru.sovmestim.gateway.dto;

import java.util.UUID;


/**
 * Stored advice answer: the audit record id together with the engine result.
 *
 * @param adviceId id of the stored audit record
 * @param result engine result for the check
 */
public record AdviceCheckResponse(UUID adviceId, AdviceResult result) { }
