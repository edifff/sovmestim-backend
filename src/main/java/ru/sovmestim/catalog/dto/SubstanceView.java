package ru.sovmestim.catalog.dto;

import java.util.UUID;

public record SubstanceView(UUID id, String name, String atcCode, String atcName, String description) {}
