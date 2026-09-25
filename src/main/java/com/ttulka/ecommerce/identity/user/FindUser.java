package com.ttulka.ecommerce.identity.user;

/**
 * Find User use-case.
 */
public interface FindUser {

    /**
     * Finds a user by username.
     *
     * @param username the username
     * @return the found user
     */
    User byUsername(Username username);
}
