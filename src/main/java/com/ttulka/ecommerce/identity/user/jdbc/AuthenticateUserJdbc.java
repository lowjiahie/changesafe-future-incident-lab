package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.AuthenticateUser;
import com.ttulka.ecommerce.identity.user.RawPassword;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC implementation for Authenticate User use-cases.
 */
@RequiredArgsConstructor
@Slf4j
class AuthenticateUserJdbc implements AuthenticateUser {

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final @NonNull PasswordEncoder passwordEncoder;
    private final @NonNull FindUserJdbc findUserJdbc;

    @Override
    public User authenticate(Username username, RawPassword password) {
        String passwordHash = jdbcTemplate.query(
                        "SELECT password_hash FROM users WHERE username = ?",
                        (rs, rowNum) -> rs.getString("password_hash"),
                        username.value())
                .stream()
                .findFirst()
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password.value(), passwordHash)) {
            log.info("Failed login attempt for username: {}", username);
            throw new InvalidCredentialsException();
        }
        return findUserJdbc.byUsername(username);
    }
}
