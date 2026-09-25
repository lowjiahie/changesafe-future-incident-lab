package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.AuthenticateUser;
import com.ttulka.ecommerce.identity.user.RawPassword;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@ContextConfiguration(classes = UserJdbcConfig.class)
@Sql(statements = "INSERT INTO users (username, password_hash, first_name, last_name, phone, address, email) VALUES " +
        "('testuser', '$2y$10$N9pXUxIaV9oQJk/lm5PQnOM/73ZaJAy1hfE9GCJPoJyAOjkb3Lju6', 'Test', 'User', '+1 555 0000', 'Test Address 123', 'test@example.com');")
class AuthenticateUserJdbcTest {

    @Autowired
    private AuthenticateUser authenticateUser;

    @Test
    void correct_credentials_authenticate_the_user() {
        User user = authenticateUser.authenticate(new Username("testuser"), new RawPassword("secret123"));

        assertThat(user.username().value()).isEqualTo("testuser");
    }

    @Test
    void wrong_password_fails_authentication() {
        assertThrows(AuthenticateUser.InvalidCredentialsException.class,
                () -> authenticateUser.authenticate(new Username("testuser"), new RawPassword("wrong-password")));
    }

    @Test
    void unknown_username_fails_authentication() {
        assertThrows(AuthenticateUser.InvalidCredentialsException.class,
                () -> authenticateUser.authenticate(new Username("nobody"), new RawPassword("secret123")));
    }
}
