package com.ttulka.ecommerce.identity.user;

/**
 * Update User Profile use-case.
 */
public interface UpdateUserProfile {

    /**
     * Updates a user's profile details.
     *
     * @param username the username of the user to update
     */
    void update(Username username, FirstName firstName, LastName lastName, Phone phone, Address address, Email email);
}
