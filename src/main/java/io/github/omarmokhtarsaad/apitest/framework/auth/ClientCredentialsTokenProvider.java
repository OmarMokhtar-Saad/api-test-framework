package io.github.omarmokhtarsaad.apitest.framework.auth;

import io.github.omarmokhtarsaad.apitest.framework.config.AuthSettings;
import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Fetches a token with the OAuth 2.0 client-credentials grant (RFC 6749 section 4.4). Every call
 * performs a request: wrap it in {@link CachingTokenProvider}.
 *
 * <p>The client secret is sent in the form body and never logged. On failure only the status code
 * and the standard {@code error} field are reported.
 */
public final class ClientCredentialsTokenProvider implements TokenProvider {

    private final AuthSettings.OAuth2ClientCredentials settings;
    private final RestAssuredConfig httpConfig;
    private final Clock clock;

    public ClientCredentialsTokenProvider(
            AuthSettings.OAuth2ClientCredentials settings, RestAssuredConfig httpConfig, Clock clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.httpConfig = Objects.requireNonNull(httpConfig, "httpConfig");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public AccessToken token() {
        Instant requestedAt = clock.instant();
        RequestSpecification request = RestAssured.given()
                .config(httpConfig)
                .contentType(ContentType.URLENC.withCharset("UTF-8"))
                .accept(ContentType.JSON)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", settings.clientId())
                .formParam("client_secret", settings.clientSecret().reveal());
        settings.scope().ifPresent(scope -> request.formParam("scope", scope));

        Response response = request.post(settings.tokenUrl());
        if (response.statusCode() != 200) {
            throw new AuthenticationException("Token endpoint %s returned HTTP %d (error: %s) for client '%s'"
                    .formatted(settings.tokenUrl(), response.statusCode(), errorCode(response), settings.clientId()));
        }
        JsonPath json = response.jsonPath();
        String accessToken = json.getString("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new AuthenticationException(
                    "Token endpoint %s returned no access_token".formatted(settings.tokenUrl()));
        }
        Long expiresIn = json.getObject("expires_in", Long.class);
        Instant expiresAt = expiresIn == null ? Instant.MAX : requestedAt.plusSeconds(expiresIn);
        return new AccessToken(Secret.of(accessToken), expiresAt);
    }

    private static String errorCode(Response response) {
        try {
            String error = response.jsonPath().getString("error");
            return error == null ? "n/a" : error;
        } catch (RuntimeException notJson) {
            return "n/a (body is not JSON)";
        }
    }
}
