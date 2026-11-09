package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import io.restassured.specification.FilterableRequestSpecification;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/** Preemptive HTTP Basic authentication (RFC 7617). */
public final class BasicAuth implements AuthStrategy {

    private final String username;
    private final Secret password;

    public BasicAuth(String username, Secret password) {
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
    }

    @Override
    public void authenticate(FilterableRequestSpecification request) {
        String raw = username + ":" + password.reveal();
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        request.header(AuthFilter.AUTHORIZATION, "Basic " + encoded);
    }
}
