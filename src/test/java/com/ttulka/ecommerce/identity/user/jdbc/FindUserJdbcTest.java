package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.FindUser;
import com.ttulka.ecommerce.identity.user.User;
import com.ttulka.ecommerce.identity.user.Username;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@JdbcTest
@ContextConfiguration(classes = UserJdbcConfig.class)
@Sql(statements = "INSERT INTO users (username, password_hash, first_name, last_name, phone, address, email) VALUES " +
        "('testuser', '$2y$10$N9pXUxIaV9oQJk/lm5PQnOM/73ZaJAy1hfE9GCJPoJyAOjkb3Lju6', 'Test', 'User', '+1 555 0000', 'Test Address 123', 'test@example.com');")
class FindUserJdbcTest {

    @Autowired
    private FindUser findUser;

    @Test
    void user_is_found_by_username() {
        User user = findUser.byUsername(new Username("testuser"));

        assertAll(
                () -> assertThat(user.username().value()).isEqualTo("testuser"),
                () -> assertThat(user.firstName().value()).isEqualTo("Test"),
                () -> assertThat(user.lastName().value()).isEqualTo("User"),
                () -> assertThat(user.phone().value()).isEqualTo("+1 555 0000"),
                () -> assertThat(user.address().value()).isEqualTo("Test Address 123"),
                () -> assertThat(user.email().value()).isEqualTo("test@example.com")
        );
    }

    @Test
    void unknown_user_is_a_null_object() {
        User user = findUser.byUsername(new Username("nobody"));

        assertThat(user.username().value()).isEqualTo("unknown");
    }
}
