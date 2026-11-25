package ru.sovmestim.advice.model;

import java.util.UUID;

/**
 * A normalized active substance together with the classification needed for matching.
 *
 * @param id catalog substance id, may be {@code null} when not resolved from the catalog
 * @param name normalized substance name
 * @param atcCode ATC code, may be {@code null}
 * @param atcName ATC group name, may be {@code null}
 * @param phg pharmacological group, may be {@code null} when the source did not provide one
 */
public record SubstanceRef(UUID id, String name, String atcCode, String atcName, String phg) { }
