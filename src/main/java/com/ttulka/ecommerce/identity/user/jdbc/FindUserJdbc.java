package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FindUser;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.springframework.jdbc.core.JdbcTemplate;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * JDBC implementation for Find User use-cases.
 */
@RequiredArgsConstructor
class FindUserJdbc implements FindUser {

    private final @NonNull JdbcTemplate jdbcTemplate;

    @Override
    public User byUsername(Username username) {
        return jdbcTemplate.query(
                        "SELECT username, first_name, last_name, phone, address, email FROM users WHERE username = ?",
                        (rs, rowNum) -> (User) new UserJdbc(
                                new Username(rs.getString("username")),
                                new FirstName(rs.getString("first_name")),
                                new LastName(rs.getString("last_name")),
                                new Phone(rs.getString("phone")),
                                new Address(rs.getString("address")),
                                new Email(rs.getString("email")),
                                jdbcTemplate),
                        username.value())
                .stream()
                .findFirst()
                .orElseGet(UnknownUser::new);
    }
}
