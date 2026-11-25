package ru.sovmestim.gateway.client;

import java.time.LocalDate;

/**
 * Public profile of a user returned by the identity service.
 *
 * @param id user id
 * @param email e-mail address
 * @param surname surname
 * @param name given name
 * @param birthDate date of birth
 */
public record UserInfo(String id, String email, String surname, String name, LocalDate birthDate) { }
