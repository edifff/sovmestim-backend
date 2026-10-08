package ru.sovmestim.patient.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ConditionRequest(
        @NotBlank String name, String mkbCode, String status, LocalDate diagnosisDate, String note) {}
