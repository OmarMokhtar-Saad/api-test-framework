package io.github.omarmokhtarsaad.apitest.framework.config;

import java.util.Optional;

/** {@code -Dkey=value} JVM system properties. Not secret-safe: visible in the process list. */
public final class SystemPropertySource implements ConfigSource {

    @Override
    public String name() {
        return "system property";
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(System.getProperty(key));
    }

    @Override
    public boolean secretSafe() {
        return false;
    }
}
