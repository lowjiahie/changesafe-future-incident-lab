package com.ttulka.ecommerce.identity.user;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * A plain-text password value as submitted at login, never persisted.
 */
@EqualsAndHashCode
@ToString(exclude = "password")
public final class RawPassword {

    private final @NonNull String password;

    public RawPassword(@NonNull String password) {
        if (password.isEmpty() || password.length() > 100) {
            throw new IllegalArgumentException("Password value is invalid!");
        }
        this.password = password;
    }

    public String value() {
        return password;
    }
}
