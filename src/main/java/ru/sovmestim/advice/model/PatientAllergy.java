package ru.sovmestim.advice.model;

/**
 * @param code an ATC code or cross-reactivity group key, may be {@code null}
 */
public record PatientAllergy(String name, String code, String severity) {}
