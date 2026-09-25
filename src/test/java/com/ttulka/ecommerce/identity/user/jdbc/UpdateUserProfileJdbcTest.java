package com.ttulka.ecommerce.identity.user.jdbc;

import com.ttulka.ecommerce.identity.user.Address;
import com.ttulka.ecommerce.identity.user.Email;
import com.ttulka.ecommerce.identity.user.FindUser;
import com.ttulka.ecommerce.identity.user.FirstName;
import com.ttulka.ecommerce.identity.user.LastName;
import com.ttulka.ecommerce.identity.user.Phone;
import com.ttulka.ecommerce.identity.user.UpdateUserProfile;
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
class UpdateUserProfileJdbcTest {

    @Autowired
    private UpdateUserProfile updateUserProfile;
    @Autowired
    private FindUser findUser;

    @Test
    void profile_is_updated() {
        updateUserProfile.update(
                new Username("testuser"),
                new FirstName("Updated"),
                new LastName("Name"),
                new Phone("+1 555 9999"),
                new Address("99 New Address"),
                new Email("updated@example.com"));

        User user = findUser.byUsername(new Username("testuser"));
        assertAll(
                () -> assertThat(user.firstName().value()).isEqualTo("Updated"),
                () -> assertThat(user.lastName().value()).isEqualTo("Name"),
                () -> assertThat(user.phone().value()).isEqualTo("+1 555 9999"),
                () -> assertThat(user.address().value()).isEqualTo("99 New Address"),
                () -> assertThat(user.email().value()).isEqualTo("updated@example.com")
        );
    }
}
