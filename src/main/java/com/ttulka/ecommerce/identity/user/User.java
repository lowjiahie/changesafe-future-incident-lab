package com.ttulka.ecommerce.identity.user;

/**
 * User entity.
 */
public interface User {

    Username username();

    FirstName firstName();

    LastName lastName();

    Phone phone();

    Address address();

    Email email();

    /**
     * Replaces the user's profile details.
     */
    void updateProfile(FirstName firstName, LastName lastName, Phone phone, Address address, Email email);
}
