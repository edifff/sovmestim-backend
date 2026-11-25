package ru.sovmestim.advice.service;

import java.time.Instant;

import ru.sovmestim.advice.model.AdviceResult;

/**
 * One advice record delivered to the gateway for a sync pull.
 *
 * @param adviceId the stored advice record id
 * @param drugName the drug the advice was produced for
 * @param createdAt the moment the advice was produced
 * @param result the decoded engine result
 */
public record AdviceDeliveryView(String adviceId, String drugName, Instant createdAt, AdviceResult result) { }
