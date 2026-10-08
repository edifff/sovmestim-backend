package ru.sovmestim.advice.model;

import java.util.UUID;

/**
 * A normalized active substance together with the classification needed for matching.
 *
 * @param phg pharmacological group, may be {@code null} when the source did not provide one
 */
public record SubstanceRef(UUID id, String name, String atcCode, String atcName, String phg) {}
