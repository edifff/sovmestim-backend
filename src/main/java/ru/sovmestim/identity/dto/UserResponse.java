package ru.sovmestim.identity.dto;

import java.time.LocalDate;

public record UserResponse(String id, String email, String surname, String name, LocalDate birthDate) {}
