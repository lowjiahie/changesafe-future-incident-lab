package com.ttulka.ecommerce.identity.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressTest {

    @Test
    void address_value_is_trimmed() {
        Address address = new Address("  12 Sakura Lane  ");
        assertThat(address.value()).isEqualTo("12 Sakura Lane");
    }

    @Test
    void address_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new Address(null));
    }

    @Test
    void address_fails_for_an_empty_string() {
        assertThrows(IllegalArgumentException.class, () -> new Address(""));
    }

    @Test
    void address_fails_when_longer_than_database_column() {
        assertThrows(IllegalArgumentException.class, () -> new Address("A".repeat(151)));
    }
}
