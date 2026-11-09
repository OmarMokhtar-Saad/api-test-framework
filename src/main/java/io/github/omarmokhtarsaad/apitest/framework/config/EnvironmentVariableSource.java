package io.github.omarmokhtarsaad.apitest.framework.config;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Process environment variables, named {@code API_} plus the upper snake case of the key:
 *
 * <pre>
 *   baseUrl                    -&gt; API_BASE_URL
 *   timeout.read.ms            -&gt; API_TIMEOUT_READ_MS
 *   auth.oauth2.clientSecret   -&gt; API_AUTH_OAUTH2_CLIENT_SECRET
 * </pre>
 *
 * <p>The preferred channel for secrets, locally (direnv, IDE run configuration) and in CI (secret
 * store).
 */
public final class EnvironmentVariableSource implements ConfigSource {

    static final String PREFIX = "API_";

    private final Map<String, String> environment;

    /** Reads the real process environment. */
    public EnvironmentVariableSource() {
        this(System.getenv());
    }

    /** Reads the given map instead of the process environment. Used by tests. */
    public EnvironmentVariableSource(Map<String, String> environment) {
        this.environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
    }

    /** Maps a configuration key to its environment variable name. */
    public static String variableName(String key) {
        String snake =
                key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replace('.', '_').replace('-', '_');
        return PREFIX + snake.toUpperCase(Locale.ROOT);
    }

    @Override
    public String name() {
        return "environment variable";
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(environment.get(variableName(key)));
    }

    @Override
    public boolean secretSafe() {
        return true;
    }
}
