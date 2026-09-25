package com.ttulka.ecommerce.identity.user;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * User Email domain primitive.
 */
@EqualsAndHashCode
@ToString
public final class Email {

    private static final String PATTERN = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";

    private final @NonNull String email;

    public Email(@NonNull String email) {
        var value = email.strip().toLowerCase();
        if (value.length() > 100 || !Pattern.matches(PATTERN, value)) {
            throw new IllegalArgumentException("Email value is invalid!");
        }
        this.email = value;
    }

    public String value() {
        return email;
    }
}
