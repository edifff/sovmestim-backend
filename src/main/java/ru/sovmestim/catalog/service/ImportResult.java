package ru.sovmestim.catalog.service;

/**
 * Counts produced by one catalog import run.
 *
 * @param catalogVersion version assigned to the imported catalog
 * @param atc number of imported ATC entries
 * @param substances number of imported active substances
 * @param medicines number of imported medicines
 */
public record ImportResult(String catalogVersion, int atc, int substances, int medicines) { }
