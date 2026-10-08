package ru.sovmestim.patient.dto;

import java.time.Instant;
import java.util.UUID;

public record AllergyView(
        UUID id, String name, String code, String severity, String symptoms, String reason, Instant updatedAt) {}
