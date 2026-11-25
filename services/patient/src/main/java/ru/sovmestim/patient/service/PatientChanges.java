package ru.sovmestim.patient.service;

import java.time.Instant;
import java.util.List;

import ru.sovmestim.patient.domain.AllergyUser;
import ru.sovmestim.patient.domain.ChronicDiseaseUser;

/**
 * One profile delta returned to the gateway for a sync pull.
 *
 * @param allergies changed allergies, including tombstones
 * @param conditions changed conditions, including tombstones
 * @param nextCursor cursor to send on the next pull
 */
public record PatientChanges(
        List<AllergyUser> allergies, List<ChronicDiseaseUser> conditions, Instant nextCursor) {
}
