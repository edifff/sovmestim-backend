package ru.sovmestim.gateway.client;

import java.util.List;

import ru.sovmestim.gateway.sync.dto.AllergyChange;
import ru.sovmestim.gateway.sync.dto.ConditionChange;

/**
 * One profile delta returned by the patient service.
 *
 * @param allergies changed allergies, including tombstones
 * @param conditions changed conditions, including tombstones
 * @param nextCursor cursor to send on the next pull
 */
public record PatientDelta(List<AllergyChange> allergies, List<ConditionChange> conditions, java.time.Instant nextCursor) { }
