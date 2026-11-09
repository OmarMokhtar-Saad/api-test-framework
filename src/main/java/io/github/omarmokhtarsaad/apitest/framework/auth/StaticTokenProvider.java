package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import java.util.Objects;

/** A fixed, pre-issued token (e.g. from {@code API_AUTH_TOKEN}). */
public final class StaticTokenProvider implements TokenProvider {

    private final AccessToken token;

    public StaticTokenProvider(Secret token) {
        this.token = AccessToken.nonExpiring(Objects.requireNonNull(token, "token"));
    }

    @Override
    public AccessToken token() {
        return token;
    }
}
