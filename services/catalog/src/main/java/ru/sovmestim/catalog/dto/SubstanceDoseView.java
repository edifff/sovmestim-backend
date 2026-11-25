package ru.sovmestim.catalog.dto;

import java.util.UUID;

/**
 * Read model for one active substance contained in a medicine.
 *
 * @param id the substance identifier
 * @param name the substance name
 * @param dosage the dosage of the substance in this medicine
 */
public record SubstanceDoseView(UUID id, String name, String dosage) { }
