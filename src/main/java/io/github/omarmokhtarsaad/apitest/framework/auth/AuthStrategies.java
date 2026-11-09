package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.github.omarmokhtarsaad.apitest.framework.config.AuthSettings;
import io.restassured.config.RestAssuredConfig;
import java.time.Clock;

/** Builds the {@link AuthStrategy} for configured {@link AuthSettings}. */
public final class AuthStrategies {

    private AuthStrategies() {}

    /**
     * The switch is exhaustive over the sealed {@link AuthSettings}: adding a variant there fails
     * compilation here until it is handled.
     *
     * @param httpConfig timeouts and mapper for requests the strategy makes itself (token fetch)
     */
    public static AuthStrategy from(AuthSettings settings, RestAssuredConfig httpConfig) {
        return switch (settings) {
            case AuthSettings.None none -> AuthStrategy.none();
            case AuthSettings.Bearer bearer -> new BearerTokenAuth(new StaticTokenProvider(bearer.token()));
            case AuthSettings.ApiKey apiKey -> new ApiKeyAuth(apiKey.header(), apiKey.value());
            case AuthSettings.Basic basic -> new BasicAuth(basic.username(), basic.password());
            case AuthSettings.OAuth2ClientCredentials oauth2 ->
                new BearerTokenAuth(new CachingTokenProvider(
                        new ClientCredentialsTokenProvider(oauth2, httpConfig, Clock.systemUTC())));
        };
    }
}
