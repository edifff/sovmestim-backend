package ru.sovmestim.advice.service;

import java.time.Instant;
import java.util.List;

/**
 * One advice delta returned for a sync pull.
 *
 * @param deliveries the advice records after the cursor
 * @param nextCursor cursor to send on the next pull
 */
public record AdviceDelta(List<AdviceDeliveryView> deliveries, Instant nextCursor) { }
