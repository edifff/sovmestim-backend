package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.List;

import ru.sovmestim.gateway.sync.dto.AdviceDelivery;

/**
 * One advice delta returned by the advice service.
 *
 * @param deliveries advice records after the cursor
 * @param nextCursor cursor to send on the next pull
 */
public record AdviceDelta(List<AdviceDelivery> deliveries, Instant nextCursor) { }
