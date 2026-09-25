package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LastNameTest {

    @Test
    void last_name_value_is_trimmed() {
        LastName lastName = new LastName("  Tanaka  ");
        assertThat(lastName.value()).isEqualTo("Tanaka");
    }

    @Test
    void last_name_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new LastName(null));
    }

    @Test
    void last_name_fails_for_an_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> new LastName(""));
    }

    @Test
    void last_name_fails_for_invalid_values() {
        assertThrows(IllegalArgumentException.class, () -> new LastName("Lee1"));
        assertThrows(IllegalArgumentException.class, () -> new LastName("A".repeat(51)));
    }

    @Test
    void last_name_accepts_valid_values() {
        assertDoesNotThrow(() -> new LastName("Tanaka"));
        assertDoesNotThrow(() -> new LastName("Lee"));
        assertDoesNotThrow(() -> new LastName("O'Neill"));
    }
}
