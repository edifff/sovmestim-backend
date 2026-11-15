package ru.sovmestim.gateway.client;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import ru.sovmestim.gateway.dto.AdviceCheckRequest;
import ru.sovmestim.gateway.dto.AdviceCheckResponse;

/**
 * Access to the advice service.
 */
public interface AdviceClient {

    /**
     * Runs a compatibility check.
     *
     * @param userId the patient's user id
     * @param request the drug identification
     * @return the stored advice
     */
    AdviceCheckResponse check(UUID userId, AdviceCheckRequest request);

    /**
     * Recomputes advice for the given courses.
     *
     * @param userId the patient's user id
     * @param courseIds the course ids
     * @return one stored response per course
     */
    List<AdviceCheckResponse> recheckCourses(UUID userId, Collection<UUID> courseIds);

    /**
     * Recomputes advice for all active courses.
     *
     * @param userId the patient's user id
     * @return one stored response per course
     */
    List<AdviceCheckResponse> recheckAll(UUID userId);

    /**
     * Returns stored advice after the cursor.
     *
     * @param userId the patient's user id
     * @param cursor exclusive change cursor
     * @return the advice delta
     */
    AdviceDelta adviceSince(UUID userId, Instant cursor);
}
