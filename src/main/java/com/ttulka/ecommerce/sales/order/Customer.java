package com.ttulka.ecommerce.sales.order;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * Customer domain primitive: the identity of the person an order belongs to.
 */
@EqualsAndHashCode
@ToString
public final class Customer {

    private final @NonNull String id;

    public Customer(@NonNull Object id) {
        var idVal = id.toString().strip();
        if (idVal.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty!");
        }
        this.id = idVal;
    }

    public String value() {
        return id;
    }
}
