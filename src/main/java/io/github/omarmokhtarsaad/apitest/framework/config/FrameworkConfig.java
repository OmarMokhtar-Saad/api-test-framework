package io.github.omarmokhtarsaad.apitest.framework.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, validated configuration for one test run. Safe to share across threads.
 *
 * <p>Built only by {@link ConfigLoader}; obtained in tests through {@link ConfigProvider#get()}.
 * {@link #toString()} is safe to log: secrets are {@link Secret}s.
 */
public record FrameworkConfig(
        String env,
        URI baseUrl,
        String basePath,
        Duration connectTimeout,
        Duration readTimeout,
        AuthSettings auth,
        HttpLogPolicy httpLog,
        int httpLogMaxBodyChars,
        Set<String> extraRedactKeys,
        int retryMax,
        long dataSeed) {

    public FrameworkConfig {
        Objects.requireNonNull(env, "env");
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(basePath, "basePath");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(readTimeout, "readTimeout");
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(httpLog, "httpLog");
        extraRedactKeys = Set.copyOf(extraRedactKeys);
    }

    /** One-line, secret-free summary printed once at start-up. */
    public String summary() {
        return "env=%s baseUrl=%s basePath='%s' auth=%s httpLog=%s retryMax=%d dataSeed=%d"
                .formatted(env, baseUrl, basePath, auth.type(), httpLog, retryMax, dataSeed);
    }
}
