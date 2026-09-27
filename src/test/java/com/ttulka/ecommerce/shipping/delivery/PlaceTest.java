package com.ttulka.ecommerce.shipping.delivery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaceTest {

    @Test
    void place_value_is_stripped() {
        Place place = new Place("  123 Main St  ");
        assertThat(place.value()).isEqualTo("123 Main St");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n", "  \t  \n  "})
    void place_rejects_blank_or_empty_string(String address) {
        assertThrows(IllegalArgumentException.class, () -> new Place(address),
                "Expected IllegalArgumentException for blank address: '" + address + "'");
    }

    @Test
    void place_rejects_address_over_100_characters() {
        assertThrows(IllegalArgumentException.class, () -> new Place("a".repeat(101)));
    }

    @Test
    void place_accepts_address_at_exactly_100_characters() {
        assertDoesNotThrow(() -> new Place("a".repeat(100)));
    }

    @Test
    void place_accepts_valid_address() {
        assertDoesNotThrow(() -> new Place("10 Downing Street, London SW1A 2AA"));
    }
}
