package io.github.omarmokhtarsaad.apitest.framework.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns raw layered values into a validated {@link FrameworkConfig}. Pure: no I/O, no globals, so
 * every rule here is unit-tested with in-memory sources.
 *
 * <p>Validation is fail-fast with actionable messages: a misconfigured run stops before the first
 * request instead of producing hundreds of confusing failures.
 */
public final class ConfigLoader {

    static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;
    static final int DEFAULT_READ_TIMEOUT_MS = 15_000;
    static final int DEFAULT_RETRY_MAX = 1;
    static final int DEFAULT_LOG_MAX_BODY_CHARS = 20_000;
    static final String DEFAULT_APIKEY_HEADER = "X-API-Key";

    private static final Pattern ENV_NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,30}");

    private final LayeredConfig layers;
    private final String env;

    public ConfigLoader(String env, LayeredConfig layers) {
        this.env = validateEnvName(env);
        this.layers = Objects.requireNonNull(layers, "layers");
    }

    /**
     * Environment names become classpath file names, so only {@code [a-z0-9-]} is accepted; this
     * also rules out path traversal such as {@code ../secrets}.
     */
    public static String validateEnvName(String env) {
        if (env == null || !ENV_NAME.matcher(env).matches()) {
            throw new ConfigurationException(
                    "Environment name '%s' is invalid: use lower-case letters, digits and '-' (e.g. qa, staging, mock)."
                            .formatted(env));
        }
        return env;
    }

    public FrameworkConfig load() {
        return new FrameworkConfig(
                env,
                httpUri(ConfigKeys.BASE_URL, required(ConfigKeys.BASE_URL)),
                normalisePath(layers.get(ConfigKeys.BASE_PATH).orElse("")),
                Duration.ofMillis(positiveInt(ConfigKeys.CONNECT_TIMEOUT_MS, DEFAULT_CONNECT_TIMEOUT_MS)),
                Duration.ofMillis(positiveInt(ConfigKeys.READ_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS)),
                auth(),
                enumValue(ConfigKeys.HTTP_LOG, HttpLogPolicy.class, HttpLogPolicy.FAILURES),
                positiveInt(ConfigKeys.HTTP_LOG_MAX_BODY_CHARS, DEFAULT_LOG_MAX_BODY_CHARS),
                csv(ConfigKeys.REDACT_EXTRA_KEYS),
                nonNegativeInt(ConfigKeys.RETRY_MAX, DEFAULT_RETRY_MAX),
                layers.get(ConfigKeys.DATA_SEED)
                        .map(raw -> parseLong(ConfigKeys.DATA_SEED, raw))
                        .orElseGet(() -> ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE)));
    }

    private AuthSettings auth() {
        String type = layers.get(ConfigKeys.AUTH_TYPE).orElse("none").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "none" -> new AuthSettings.None();
            case "bearer" -> new AuthSettings.Bearer(requiredSecret(ConfigKeys.AUTH_TOKEN));
            case "apikey" ->
                new AuthSettings.ApiKey(
                        layers.get(ConfigKeys.AUTH_APIKEY_HEADER).orElse(DEFAULT_APIKEY_HEADER),
                        requiredSecret(ConfigKeys.AUTH_APIKEY_VALUE));
            case "basic" ->
                new AuthSettings.Basic(
                        required(ConfigKeys.AUTH_BASIC_USERNAME), requiredSecret(ConfigKeys.AUTH_BASIC_PASSWORD));
            case "oauth2" ->
                new AuthSettings.OAuth2ClientCredentials(
                        httpUri(ConfigKeys.AUTH_OAUTH2_TOKEN_URL, required(ConfigKeys.AUTH_OAUTH2_TOKEN_URL)),
                        required(ConfigKeys.AUTH_OAUTH2_CLIENT_ID),
                        requiredSecret(ConfigKeys.AUTH_OAUTH2_CLIENT_SECRET),
                        layers.get(ConfigKeys.AUTH_OAUTH2_SCOPE));
            default ->
                throw new ConfigurationException("'%s' must be one of none, bearer, apikey, basic, oauth2. Got: '%s'"
                        .formatted(ConfigKeys.AUTH_TYPE, type));
        };
    }

    private String required(String key) {
        return layers.get(key).orElseThrow(() -> new ConfigurationException(missing(key, false)));
    }

    private Secret requiredSecret(String key) {
        return layers.secret(key).orElseThrow(() -> new ConfigurationException(missing(key, true)));
    }

    private String missing(String key, boolean secret) {
        String where = secret
                ? """
                    1. Environment variable  %s
                    2. Secrets file          src/test/resources/env/%s.secrets.properties (git-ignored)""".formatted(EnvironmentVariableSource.variableName(key), env)
                : """
                    1. JVM system property   -D%s=...
                    2. Environment variable  %s
                    3. Environment file      src/test/resources/env/%s.properties""".formatted(key, EnvironmentVariableSource.variableName(key), env);
        return "Required %s '%s' is not set for environment '%s'. Set it in one of (highest precedence first):%n%s%nSee docs/CONFIGURATION.md."
                .formatted(secret ? "secret" : "configuration", key, env, where);
    }

    private static URI httpUri(String key, String value) {
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new ConfigurationException("'%s' is not a valid URI: '%s'".formatted(key, value), e);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (uri.getHost() == null || !(scheme.equals("http") || scheme.equals("https"))) {
            throw new ConfigurationException(
                    "'%s' must be an absolute http(s) URL such as https://api.example.com. Got: '%s'"
                            .formatted(key, value));
        }
        if (uri.getUserInfo() != null) {
            throw new ConfigurationException(
                    "'%s' must not embed credentials (user:password@host). Use the auth.* keys.".formatted(key));
        }
        return uri;
    }

    private int positiveInt(String key, int defaultValue) {
        int value = layers.get(key).map(raw -> parseInt(key, raw)).orElse(defaultValue);
        if (value <= 0) {
            throw new ConfigurationException("'%s' must be greater than zero. Got: %d".formatted(key, value));
        }
        return value;
    }

    private int nonNegativeInt(String key, int defaultValue) {
        int value = layers.get(key).map(raw -> parseInt(key, raw)).orElse(defaultValue);
        if (value < 0) {
            throw new ConfigurationException("'%s' must be zero or more. Got: %d".formatted(key, value));
        }
        return value;
    }

    private static int parseInt(String key, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("'%s' must be an integer. Got: '%s'".formatted(key, raw), e);
        }
    }

    private static long parseLong(String key, String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("'%s' must be an integer. Got: '%s'".formatted(key, raw), e);
        }
    }

    private <E extends Enum<E>> E enumValue(String key, Class<E> type, E defaultValue) {
        return layers.get(key)
                .map(raw -> {
                    try {
                        return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        throw new ConfigurationException(
                                "'%s' must be one of %s. Got: '%s'"
                                        .formatted(key, Arrays.toString(type.getEnumConstants()), raw),
                                e);
                    }
                })
                .orElse(defaultValue);
    }

    private Set<String> csv(String key) {
        return layers.get(key).stream()
                .flatMap(raw -> Arrays.stream(raw.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Leading slash, no trailing slash, or empty. */
    static String normalisePath(String path) {
        String p = path.trim();
        if (p.isEmpty() || p.equals("/")) {
            return "";
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
