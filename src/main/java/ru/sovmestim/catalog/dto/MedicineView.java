package ru.sovmestim.catalog.dto;

import java.util.List;
import java.util.UUID;

public record MedicineView(
        UUID id, String name, String brand, String formRelease, List<SubstanceDoseView> substances) {

    public record SubstanceDoseView(UUID id, String name, String dosage) {}
}
