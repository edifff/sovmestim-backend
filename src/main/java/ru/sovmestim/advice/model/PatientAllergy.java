package ru.sovmestim.advice.model;

/**
 * A drug allergy declared by the patient.
 *
 * @param name allergen substance name as entered by the user
 * @param code an ATC code or cross-reactivity group key, may be {@code null}
 * @param severity allergy severity as entered by the user, may be {@code null}
 */
public record PatientAllergy(String name, String code, String severity) { }
