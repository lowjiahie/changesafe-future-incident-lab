package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneTest {

    @Test
    void phone_value_is_trimmed() {
        Phone phone = new Phone("  +1 555 0101  ");
        assertThat(phone.value()).isEqualTo("+1 555 0101");
    }

    @Test
    void phone_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new Phone(null));
    }

    @Test
    void phone_fails_for_invalid_values() {
        assertThrows(IllegalArgumentException.class, () -> new Phone(""));
        assertThrows(IllegalArgumentException.class, () -> new Phone("abc"));
        assertThrows(IllegalArgumentException.class, () -> new Phone("12"));
    }

    @Test
    void phone_accepts_valid_values() {
        assertDoesNotThrow(() -> new Phone("+1 555 0101"));
        assertDoesNotThrow(() -> new Phone("(555) 010-1234"));
    }
}
