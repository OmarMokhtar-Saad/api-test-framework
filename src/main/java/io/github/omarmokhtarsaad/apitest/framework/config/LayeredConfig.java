package io.github.omarmokhtarsaad.apitest.framework.config;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An ordered stack of {@link ConfigSource}s; the first source with a non-blank value wins.
 *
 * <p>A blank value counts as "not set", so build tools can forward empty properties without
 * shadowing lower layers.
 */
public final class LayeredConfig {

    private final List<ConfigSource> sources;

    public LayeredConfig(List<ConfigSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    }

    /** First non-blank, trimmed value for {@code key}. */
    public Optional<String> get(String key) {
        return find(key).map(Resolved::value);
    }

    /** The value and the source that supplied it, for diagnostics. */
    public Optional<Resolved> find(String key) {
        for (ConfigSource source : sources) {
            Optional<String> value = source.get(key).map(String::trim).filter(v -> !v.isEmpty());
            if (value.isPresent()) {
                return Optional.of(new Resolved(key, value.get(), source));
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves a secret. The winning source must be secret-safe; otherwise this fails loudly rather
     * than silently using a value that has already leaked into a process list or git history.
     */
    public Optional<Secret> secret(String key) {
        Optional<Resolved> resolved = find(key);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }
        Resolved r = resolved.get();
        if (!r.source().secretSafe()) {
            throw new ConfigurationException(
                    """
                    Secret '%s' was supplied via %s, which is not allowed because it leaks the value.
                    Provide it as environment variable %s, or in the git-ignored file
                    src/test/resources/env/<env>.secrets.properties.""".formatted(key, r.source().name(), EnvironmentVariableSource.variableName(key)));
        }
        return Optional.of(Secret.of(r.value()));
    }

    /** A resolved key: value plus provenance. */
    public record Resolved(String key, String value, ConfigSource source) {}
}
