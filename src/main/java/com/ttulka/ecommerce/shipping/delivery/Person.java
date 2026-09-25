package com.ttulka.ecommerce.shipping.delivery;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * Delivery Person entity.
 */
@EqualsAndHashCode
@ToString
public final class Person {

    // Names do not have a universal capitalization or word-count rule.
    private static final String PATTERN = "[\\p{L}]+(?:[ '\\-][\\p{L}]+)*";

    private final @NonNull String name;

    public Person(@NonNull String name) {
        var nameVal = name.strip();
        if (nameVal.isBlank()) {
            throw new IllegalArgumentException("Person cannot be empty!");
        }
        if (nameVal.length() > 50 || !Pattern.matches(PATTERN, nameVal)) {
            throw new IllegalArgumentException("Person value is invalid!");
        }
        this.name = nameVal;
    }

    public String value() {
        return name;
    }
}
