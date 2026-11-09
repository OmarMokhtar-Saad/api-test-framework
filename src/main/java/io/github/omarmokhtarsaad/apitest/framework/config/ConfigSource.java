package io.github.omarmokhtarsaad.apitest.framework.config;

import java.util.Optional;

/**
 * One layer of configuration (system properties, environment variables, a properties file...).
 *
 * <p>New sources (Vault, a cloud secret manager, a JSON file) are added by implementing this
 * interface and registering the source in {@link ConfigProvider}; no existing class changes
 * (Open/Closed principle).
 */
public interface ConfigSource {

    /** Human-readable name used in diagnostics, e.g. {@code "file env/qa.properties"}. */
    String name();

    /** Raw value for {@code key}, or empty. Implementations must not trim or interpret it. */
    Optional<String> get(String key);

    /**
     * Whether secrets may be read from this source. System properties are visible to every user of
     * the machine through the process list, and committed environment files end up in git history,
     * so both return {@code false}.
     */
    boolean secretSafe();
}
