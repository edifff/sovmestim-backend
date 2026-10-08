package ru.sovmestim.intake.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CourseMedicineView(
        UUID id,
        String medicineName,
        String brand,
        String dosage,
        String frequency,
        LocalDate startDate,
        String status,
        Instant updatedAt) {}
