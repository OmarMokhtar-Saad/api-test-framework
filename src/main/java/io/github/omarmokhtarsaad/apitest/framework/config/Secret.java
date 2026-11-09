package io.github.omarmokhtarsaad.apitest.framework.config;

import java.util.Objects;

/**
 * A sensitive configuration value. {@link #toString()} never reveals it, so a {@code Secret} can be
 * logged, put in an exception message, or shown by a debugger without leaking.
 *
 * <p>Call {@link #reveal()} only at the exact point the raw value is sent over the wire.
 */
public final class Secret {

    private static final String MASK = "****";

    private final String value;

    private Secret(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static Secret of(String value) {
        return new Secret(value);
    }

    /** Returns the raw value. Never log the result. */
    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return MASK;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Secret secret && value.equals(secret.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
