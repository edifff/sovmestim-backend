package ru.sovmestim.identity.dto;

import java.time.LocalDate;

/**
 * Public view of a user's profile.
 *
 * @param id identifier of the user.
 * @param email e-mail address of the user.
 * @param surname surname of the user.
 * @param name given name of the user.
 * @param birthDate date of birth of the user.
 */
public record UserResponse(String id, String email, String surname, String name, LocalDate birthDate) { }
