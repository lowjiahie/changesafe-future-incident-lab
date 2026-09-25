package com.ttulka.ecommerce.identity.user;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * User Address domain primitive.
 */
@EqualsAndHashCode
@ToString
public final class Address {

    private final @NonNull String address;

    public Address(@NonNull String address) {
        var value = address.strip();
        if (value.isBlank() || value.length() > 150) {
            throw new IllegalArgumentException("Address value is invalid!");
        }
        this.address = value;
    }

    public String value() {
        return address;
    }
}
