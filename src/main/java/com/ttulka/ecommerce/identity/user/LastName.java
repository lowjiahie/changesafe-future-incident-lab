package com.ttulka.ecommerce.identity.user;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * User LastName domain primitive.
 */
@EqualsAndHashCode
@ToString
public final class LastName {

    // Names do not have a universal capitalization or word-count rule.
    private static final String PATTERN = "[\\p{L}]+(?:[ '\\-][\\p{L}]+)*";

    private final @NonNull String lastName;

    public LastName(@NonNull String lastName) {
        var value = lastName.strip();
        if (value.isBlank() || value.length() > 50 || !Pattern.matches(PATTERN, value)) {
            throw new IllegalArgumentException("Last name value is invalid!");
        }
        this.lastName = value;
    }

    public String value() {
        return lastName;
    }
}
