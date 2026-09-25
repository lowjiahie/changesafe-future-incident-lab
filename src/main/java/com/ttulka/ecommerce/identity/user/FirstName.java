package com.ttulka.ecommerce.identity.user;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * User FirstName domain primitive.
 */
@EqualsAndHashCode
@ToString
public final class FirstName {

    // Names do not have a universal capitalization or word-count rule.
    private static final String PATTERN = "[\\p{L}]+(?:[ '\\-][\\p{L}]+)*";

    private final @NonNull String firstName;

    public FirstName(@NonNull String firstName) {
        var value = firstName.strip();
        if (value.isBlank() || value.length() > 50 || !Pattern.matches(PATTERN, value)) {
            throw new IllegalArgumentException("First name value is invalid!");
        }
        this.firstName = value;
    }

    public String value() {
        return firstName;
    }
}
