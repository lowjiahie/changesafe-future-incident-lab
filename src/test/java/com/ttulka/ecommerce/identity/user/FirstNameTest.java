package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirstNameTest {

    @Test
    void first_name_value_is_trimmed() {
        FirstName firstName = new FirstName("  Ami  ");
        assertThat(firstName.value()).isEqualTo("Ami");
    }

    @Test
    void first_name_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new FirstName(null));
    }

    @Test
    void first_name_fails_for_an_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> new FirstName(""));
    }

    @Test
    void first_name_fails_for_invalid_values() {
        assertThrows(IllegalArgumentException.class, () -> new FirstName("Ami1"));
        assertThrows(IllegalArgumentException.class, () -> new FirstName("A".repeat(51)));
    }

    @Test
    void first_name_accepts_valid_values() {
        assertDoesNotThrow(() -> new FirstName("Ami"));
        assertDoesNotThrow(() -> new FirstName("Anne-Marie"));
        assertDoesNotThrow(() -> new FirstName("O'Neill"));
    }
}
