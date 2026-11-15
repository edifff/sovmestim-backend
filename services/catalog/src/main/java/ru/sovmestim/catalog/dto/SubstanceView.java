package ru.sovmestim.catalog.dto;

import java.util.UUID;

/**
 * Read model for an active substance returned by catalog search.
 *
 * @param id the substance identifier
 * @param name the substance name
 * @param atcCode the ATC classification code
 * @param atcName the ATC classification description
 * @param description the substance description
 */
public record SubstanceView(UUID id, String name, String atcCode, String atcName, String description) { }
