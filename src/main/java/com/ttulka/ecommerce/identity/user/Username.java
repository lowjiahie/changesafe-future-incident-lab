package com.ttulka.ecommerce.identity.user;

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * User Username domain primitive.
 */
@EqualsAndHashCode
@ToString
public final class Username {

    private static final String PATTERN = "[a-z0-9](?:[a-z0-9._-]*[a-z0-9])?";

    private final @NonNull String username;

    public Username(@NonNull String username) {
        var usernameVal = username.strip().toLowerCase();
        if (usernameVal.length() < 3 || usernameVal.length() > 30 || !Pattern.matches(PATTERN, usernameVal)) {
            throw new IllegalArgumentException("Username value is invalid!");
        }
        this.username = usernameVal;
    }

    public String value() {
        return username;
    }
}
