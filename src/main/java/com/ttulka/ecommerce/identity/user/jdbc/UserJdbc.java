package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC implementation of User entity.
 */
@RequiredArgsConstructor
@EqualsAndHashCode(of = "username")
@ToString(of = "username")
@Slf4j
final class UserJdbc implements User {

    private final @NonNull Username username;
    private final @NonNull FirstName firstName;
    private final @NonNull LastName lastName;
    private final @NonNull Phone phone;
    private final @NonNull Address address;
    private final @NonNull Email email;

    private final @NonNull JdbcTemplate jdbcTemplate;

    @Override
    public Username username() {
        return username;
    }

    @Override
    public FirstName firstName() {
        return firstName;
    }

    @Override
    public LastName lastName() {
        return lastName;
    }

    @Override
    public Phone phone() {
        return phone;
    }

    @Override
    public Address address() {
        return address;
    }

    @Override
    public Email email() {
        return email;
    }

    @Override
    public void updateProfile(FirstName firstName, LastName lastName, Phone phone, Address address, Email email) {
        jdbcTemplate.update(
                "UPDATE users SET first_name = ?, last_name = ?, phone = ?, address = ?, email = ? WHERE username = ?",
                firstName.value(), lastName.value(), phone.value(), address.value(), email.value(), username.value());

        log.info("User profile updated: {}", username);
    }
}
