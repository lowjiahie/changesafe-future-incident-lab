package com.ttulka.ecommerce.sales.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTest {

    @Test
    void customer_value() {
        assertEquals("ami", new Customer("ami").value());
    }

    @Test
    void customer_value_is_stripped() {
        assertEquals("ami", new Customer(" ami ").value());
    }

    @Test
    void customer_fails_for_a_null_value() {
        assertThrows(IllegalArgumentException.class, () -> new Customer(null));
    }

    @Test
    void customer_fails_for_an_empty_value() {
        assertThrows(IllegalArgumentException.class, () -> new Customer("  "));
    }
}
