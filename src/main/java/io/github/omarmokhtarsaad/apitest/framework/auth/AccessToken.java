package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** A bearer token and the instant it stops being valid. {@link Instant#MAX} means "does not expire". */
public record AccessToken(Secret value, Instant expiresAt) {

    public AccessToken {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public static AccessToken nonExpiring(Secret value) {
        return new AccessToken(value, Instant.MAX);
    }

    /** True if the token is still valid at {@code now + skew}. */
    public boolean isUsableAt(Instant now, Duration skew) {
        return expiresAt.equals(Instant.MAX) || now.plus(skew).isBefore(expiresAt);
    }
}
