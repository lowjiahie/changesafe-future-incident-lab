package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.UpdateUserProfile;
import com.ttulka.ecommerce.identity.user.Username;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * JDBC implementation for Update User Profile use-cases.
 */
@RequiredArgsConstructor
class UpdateUserProfileJdbc implements UpdateUserProfile {

    private final @NonNull FindUserJdbc findUserJdbc;

    @Override
    public void update(Username username, FirstName firstName, LastName lastName, Phone phone, Address address, Email email) {
        findUserJdbc.byUsername(username)
                .updateProfile(firstName, lastName, phone, address, email);
    }
}
