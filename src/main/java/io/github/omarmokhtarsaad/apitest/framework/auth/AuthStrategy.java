package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.restassured.specification.FilterableRequestSpecification;

/**
 * Adds credentials to an outgoing request. One implementation per scheme (Strategy pattern): the
 * HTTP client depends on this interface only, so a new scheme (HMAC signing, a custom header, ...)
 * is a new class, not an edit to the client (Open/Closed, Dependency Inversion).
 *
 * <p>Implementations must be thread-safe: one instance serves every parallel test.
 */
@FunctionalInterface
public interface AuthStrategy {

    /** Applies credentials to {@code request}. Called once per request, just before it is sent. */
    void authenticate(FilterableRequestSpecification request);

    /** A strategy that adds nothing, for anonymous endpoints and negative tests. */
    static AuthStrategy none() {
        return request -> {};
    }
}
