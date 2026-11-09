package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.util.Objects;

/**
 * Adapts an {@link AuthStrategy} to a REST Assured filter. Runs first, so later filters (the
 * exchange recorder) see the request exactly as sent.
 *
 * <p>If the test already set an {@code Authorization} header (e.g. a deliberately invalid token in a
 * negative test), that header is left untouched.
 */
public final class AuthFilter implements OrderedFilter {

    static final String AUTHORIZATION = "Authorization";

    private final AuthStrategy strategy;

    public AuthFilter(AuthStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public Response filter(
            FilterableRequestSpecification request, FilterableResponseSpecification response, FilterContext ctx) {
        if (!request.getHeaders().hasHeaderWithName(AUTHORIZATION)) {
            strategy.authenticate(request);
        }
        return ctx.next(request, response);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
