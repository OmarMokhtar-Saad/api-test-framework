package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.restassured.specification.FilterableRequestSpecification;
import java.util.Objects;

/** {@code Authorization: Bearer <token>}, the token coming from a {@link TokenProvider}. */
public final class BearerTokenAuth implements AuthStrategy {

    private final TokenProvider tokens;

    public BearerTokenAuth(TokenProvider tokens) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    @Override
    public void authenticate(FilterableRequestSpecification request) {
        request.header(
                AuthFilter.AUTHORIZATION, "Bearer " + tokens.token().value().reveal());
    }
}
