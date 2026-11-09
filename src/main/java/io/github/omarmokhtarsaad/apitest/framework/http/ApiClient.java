package io.github.omarmokhtarsaad.apitest.framework.http;

import io.github.omarmokhtarsaad.apitest.framework.auth.AuthFilter;
import io.github.omarmokhtarsaad.apitest.framework.auth.AuthStrategies;
import io.github.omarmokhtarsaad.apitest.framework.auth.AuthStrategy;
import io.github.omarmokhtarsaad.apitest.framework.config.AuthSettings;
import io.github.omarmokhtarsaad.apitest.framework.config.FrameworkConfig;
import io.github.omarmokhtarsaad.apitest.framework.config.HttpLogPolicy;
import io.github.omarmokhtarsaad.apitest.framework.logging.HttpExchangeRecorder;
import io.github.omarmokhtarsaad.apitest.framework.logging.Redactor;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entry point for every HTTP call a test makes. Immutable and thread-safe: {@link #request()} builds
 * a fresh specification per call, so parallel tests never share mutable request state.
 *
 * <p>Variants are derived, never mutated: {@code client.withoutAuth()} returns a new client and
 * leaves {@code client} unchanged.
 *
 * <pre>{@code
 * Response r = api().request().pathParam("id", 1).get("/users/{id}");
 * }</pre>
 */
public final class ApiClient {

    private final URI baseUri;
    private final String basePath;
    private final RestAssuredConfig httpConfig;
    private final AuthStrategy auth;
    private final List<Filter> filters;

    public ApiClient(
            URI baseUri, String basePath, RestAssuredConfig httpConfig, AuthStrategy auth, List<Filter> filters) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
        this.basePath = Objects.requireNonNull(basePath, "basePath");
        this.httpConfig = Objects.requireNonNull(httpConfig, "httpConfig");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.filters = List.copyOf(filters);
    }

    /** A client wired from configuration: timeouts, authentication and redacted exchange capture. */
    public static ApiClient from(FrameworkConfig config) {
        RestAssuredConfig httpConfig = HttpConfigs.restAssuredConfig(config.connectTimeout(), config.readTimeout());
        List<Filter> filters = new ArrayList<>();
        if (config.httpLog() != HttpLogPolicy.NONE) {
            filters.add(new HttpExchangeRecorder(redactorFor(config), config.httpLogMaxBodyChars()));
        }
        return new ApiClient(
                config.baseUrl(),
                config.basePath(),
                httpConfig,
                AuthStrategies.from(config.auth(), httpConfig),
                filters);
    }

    /** The redactor for {@code config}: defaults, {@code redact.extraKeys}, and a custom API-key header. */
    public static Redactor redactorFor(FrameworkConfig config) {
        Set<String> extra = new HashSet<>(config.extraRedactKeys());
        if (config.auth() instanceof AuthSettings.ApiKey apiKey) {
            extra.add(apiKey.header());
        }
        return new Redactor(extra);
    }

    /** A new request with base URI, base path, JSON content type, auth and filters applied. */
    public RequestSpecification request() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri.toString())
                .setBasePath(basePath)
                .setConfig(httpConfig)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AuthFilter(auth));
        filters.forEach(builder::addFilter);
        return RestAssured.given().spec(builder.build());
    }

    /** The same client with a different authentication strategy (e.g. a second user). */
    public ApiClient withAuth(AuthStrategy newAuth) {
        return new ApiClient(baseUri, basePath, httpConfig, newAuth, filters);
    }

    /** The same client sending no credentials, for 401 tests. */
    public ApiClient withoutAuth() {
        return withAuth(AuthStrategy.none());
    }

    /** The same client with one more filter. */
    public ApiClient withFilter(Filter filter) {
        List<Filter> more = new ArrayList<>(filters);
        more.add(Objects.requireNonNull(filter, "filter"));
        return new ApiClient(baseUri, basePath, httpConfig, auth, more);
    }

    public URI baseUri() {
        return baseUri;
    }
}
