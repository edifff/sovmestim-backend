package ru.sovmestim.patient.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConditionView(
        UUID id,
        String name,
        String mkbCode,
        String status,
        LocalDate diagnosisDate,
        String note,
        Instant updatedAt) {}
