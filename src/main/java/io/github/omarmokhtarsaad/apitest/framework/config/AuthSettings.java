package io.github.omarmokhtarsaad.apitest.framework.config;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * How requests authenticate, selected by {@code auth.type}. A sealed hierarchy: the compiler forces
 * every consumer ({@code AuthStrategies}) to handle each variant, and invalid combinations (a
 * bearer config without a token) cannot be constructed.
 */
public sealed interface AuthSettings {

    /** Short label for diagnostics. Never includes secret material. */
    String type();

    /** No authentication. */
    record None() implements AuthSettings {
        @Override
        public String type() {
            return "none";
        }
    }

    /** A static bearer token, e.g. a long-lived service token. */
    record Bearer(Secret token) implements AuthSettings {
        public Bearer {
            Objects.requireNonNull(token, "token");
        }

        @Override
        public String type() {
            return "bearer";
        }
    }

    /** An API key sent in a header. */
    record ApiKey(String header, Secret value) implements AuthSettings {
        public ApiKey {
            Objects.requireNonNull(header, "header");
            Objects.requireNonNull(value, "value");
        }

        @Override
        public String type() {
            return "apikey";
        }
    }

    /** HTTP Basic. */
    record Basic(String username, Secret password) implements AuthSettings {
        public Basic {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }

        @Override
        public String type() {
            return "basic";
        }
    }

    /** OAuth 2.0 client-credentials grant; the token is fetched, cached and refreshed on expiry. */
    record OAuth2ClientCredentials(URI tokenUrl, String clientId, Secret clientSecret, Optional<String> scope)
            implements AuthSettings {
        public OAuth2ClientCredentials {
            Objects.requireNonNull(tokenUrl, "tokenUrl");
            Objects.requireNonNull(clientId, "clientId");
            Objects.requireNonNull(clientSecret, "clientSecret");
            Objects.requireNonNull(scope, "scope");
        }

        @Override
        public String type() {
            return "oauth2";
        }
    }
}
