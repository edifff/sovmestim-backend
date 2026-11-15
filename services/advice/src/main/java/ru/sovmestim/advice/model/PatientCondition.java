package ru.sovmestim.advice.model;

/**
 * A medical condition declared for the patient.
 *
 * @param name condition name as entered by the user
 * @param mkbCode MKB (ICD) code of the condition, may be {@code null}
 */
public record PatientCondition(String name, String mkbCode) { }
