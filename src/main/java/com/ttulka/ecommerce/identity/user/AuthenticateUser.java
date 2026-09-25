package com.ttulka.ecommerce.identity.user;

/**
 * Authenticate User use-case.
 */
public interface AuthenticateUser {

    /**
     * Authenticates a user by username and password.
     *
     * @param username the username
     * @param password the plain-text password
     * @return the authenticated user
     * @throws InvalidCredentialsException when the username is unknown or the password does not match
     */
    User authenticate(Username username, RawPassword password);

    /**
     * InvalidCredentialsException is thrown when authentication fails.
     */
    final class InvalidCredentialsException extends IllegalArgumentException {
    }
}
