package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import lombok.ToString;

/**
 * Null object implementation for User entity.
 */
@ToString
final class UnknownUser implements User {

    @Override
    public Username username() {
        return new Username("unknown");
    }

    @Override
    public FirstName firstName() {
        return new FirstName("Unknown");
    }

    @Override
    public LastName lastName() {
        return new LastName("User");
    }

    @Override
    public Phone phone() {
        return new Phone("0000");
    }

    @Override
    public Address address() {
        return new Address("unknown");
    }

    @Override
    public Email email() {
        return new Email("unknown@example.com");
    }

    @Override
    public void updateProfile(FirstName firstName, LastName lastName, Phone phone, Address address, Email email) {
        // do nothing
    }
}
