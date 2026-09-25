package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsernameTest {

    @Test
    void username_value() {
        Username username = new Username("ami");
        assertThat(username.value()).isEqualTo("ami");
    }

    @Test
    void username_is_trimmed_and_lower_cased() {
        Username username = new Username("  Ami  ");
        assertThat(username.value()).isEqualTo("ami");
    }

    @Test
    void username_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new Username(null));
    }

    @Test
    void username_fails_when_too_short() {
        assertThrows(IllegalArgumentException.class, () -> new Username("ab"));
    }

    @Test
    void username_fails_when_too_long() {
        assertThrows(IllegalArgumentException.class, () -> new Username("a".repeat(31)));
    }

    @Test
    void username_fails_for_invalid_characters() {
        assertThrows(IllegalArgumentException.class, () -> new Username("ami smith"));
        assertThrows(IllegalArgumentException.class, () -> new Username("ami@example"));
        assertThrows(IllegalArgumentException.class, () -> new Username(".ami"));
    }

    @Test
    void username_accepts_valid_values() {
        assertDoesNotThrow(() -> new Username("ami"));
        assertDoesNotThrow(() -> new Username("jason"));
        assertDoesNotThrow(() -> new Username("j.smith-99"));
    }
}
