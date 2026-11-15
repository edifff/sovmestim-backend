package ru.sovmestim.gateway.client;

import java.util.UUID;

/**
 * Access to the identity service.
 */
public interface IdentityClient {

    /**
     * Requests a one-time login code.
     *
     * @param email the e-mail address
     * @return the issued code details
     */
    RequestCodeResult requestCode(String email);

    /**
     * Verifies a one-time code and issues tokens.
     *
     * @param email the e-mail address
     * @param code the submitted code
     * @return the issued tokens
     */
    TokenResult verifyCode(String email, String code);

    /**
     * Rotates a refresh token.
     *
     * @param refreshToken the presented refresh token
     * @return the issued tokens
     */
    TokenResult refresh(String refreshToken);

    /**
     * Loads the public profile of a user.
     *
     * @param userId the user id
     * @return the user profile
     */
    UserInfo user(UUID userId);
}
