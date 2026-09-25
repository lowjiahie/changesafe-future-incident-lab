package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    void email_value_is_trimmed_and_lower_cased() {
        Email email = new Email("  Ami@Example.COM  ");
        assertThat(email.value()).isEqualTo("ami@example.com");
    }

    @Test
    void email_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    void email_fails_for_invalid_values() {
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new Email("missing@domain"));
        assertThrows(IllegalArgumentException.class, () -> new Email("@example.com"));
    }

    @Test
    void email_accepts_valid_values() {
        assertDoesNotThrow(() -> new Email("ami@example.com"));
        assertDoesNotThrow(() -> new Email("jason.lee+demo@example.co.uk"));
    }
}
