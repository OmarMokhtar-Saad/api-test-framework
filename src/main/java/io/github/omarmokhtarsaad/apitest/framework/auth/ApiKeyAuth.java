package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import io.restassured.specification.FilterableRequestSpecification;
import java.util.Objects;

/** An API key in a request header, e.g. {@code X-API-Key: <key>}. */
public final class ApiKeyAuth implements AuthStrategy {

    private final String header;
    private final Secret key;

    public ApiKeyAuth(String header, Secret key) {
        this.header = Objects.requireNonNull(header, "header");
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public void authenticate(FilterableRequestSpecification request) {
        if (!request.getHeaders().hasHeaderWithName(header)) {
            request.header(header, key.reveal());
        }
    }
}
