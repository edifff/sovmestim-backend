package ru.sovmestim.catalog.dto;

import java.util.List;
import java.util.UUID;

/**
 * Read model for a medicine returned by catalog search.
 *
 * @param id the medicine identifier
 * @param name the medicine name
 * @param brand the trade mark brand name
 * @param formRelease the release form name
 * @param substances the active substances of the medicine with their dosages
 */
public record MedicineView(
        UUID id, String name, String brand, String formRelease, List<SubstanceDoseView> substances) {

    /**
     * Read model for one active substance contained in a medicine.
     *
     * @param id the substance identifier
     * @param name the substance name
     * @param dosage the dosage of the substance in this medicine
     */
    public record SubstanceDoseView(UUID id, String name, String dosage) { }
}
