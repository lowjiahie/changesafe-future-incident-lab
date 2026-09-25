package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawPasswordTest {

    @Test
    void raw_password_value() {
        RawPassword password = new RawPassword("Ami@1234");
        assertThat(password.value()).isEqualTo("Ami@1234");
    }

    @Test
    void raw_password_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(null));
    }

    @Test
    void raw_password_fails_for_an_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(""));
    }

    @Test
    void raw_password_fails_when_too_long() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword("a".repeat(101)));
    }
}
