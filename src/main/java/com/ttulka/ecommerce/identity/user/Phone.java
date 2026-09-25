package com.ttulka.ecommerce.identity.user;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * User Phone domain primitive.
 */
@EqualsAndHashCode
@ToString
public final class Phone {

    private static final String PATTERN = "\\+?[0-9 ()-]{4,20}";

    private final @NonNull String phone;

    public Phone(@NonNull String phone) {
        var value = phone.strip();
        if (!Pattern.matches(PATTERN, value)) {
            throw new IllegalArgumentException("Phone value is invalid!");
        }
        this.phone = value;
    }

    public String value() {
        return phone;
    }
}
