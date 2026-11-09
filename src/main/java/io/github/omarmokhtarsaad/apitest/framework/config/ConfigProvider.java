package io.github.omarmokhtarsaad.apitest.framework.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide access point for the run's {@link FrameworkConfig}.
 *
 * <p>Layers, highest precedence first:
 *
 * <ol>
 *   <li>JVM system property ({@code -DbaseUrl=...}) - never for secrets
 *   <li>Environment variable ({@code API_BASE_URL})
 *   <li>{@code env/<env>.secrets.properties} - git-ignored, the only file allowed to hold secrets
 *   <li>{@code env/<env>.properties} - committed, per-environment, no secrets
 *   <li>{@code env/common.properties} - committed, shared by all environments, no secrets
 * </ol>
 *
 * <p>The environment comes from {@code -Denv} / {@code API_ENV} and defaults to {@value
 * #DEFAULT_ENV}, the offline stub. Loading is lazy (first call to {@link #get()}), so listeners that
 * prepare the environment (e.g. start a stub server) run first.
 */
public final class ConfigProvider {

    public static final String DEFAULT_ENV = "mock";

    private static final Logger LOG = LoggerFactory.getLogger(ConfigProvider.class);

    private ConfigProvider() {}

    /** The validated configuration, loaded once per JVM on first use. Thread-safe. */
    public static FrameworkConfig get() {
        return Holder.INSTANCE;
    }

    /**
     * Only system properties and environment variables, uncached. For values needed before the full
     * configuration may be loaded (the environment name, the thread count).
     */
    public static LayeredConfig bootstrap() {
        return new LayeredConfig(List.of(new SystemPropertySource(), new EnvironmentVariableSource()));
    }

    /** The environment selected for this run, validated. */
    public static String activeEnv() {
        return ConfigLoader.validateEnvName(bootstrap().get(ConfigKeys.ENV).orElse(DEFAULT_ENV));
    }

    /** All layers for {@code env}, in precedence order. */
    public static LayeredConfig layersFor(String env) {
        return new LayeredConfig(List.of(
                new SystemPropertySource(),
                new EnvironmentVariableSource(),
                PropertiesSource.fromClasspath("env/" + env + ".secrets.properties", true),
                PropertiesSource.fromClasspath("env/" + env + ".properties", false),
                PropertiesSource.fromClasspath("env/common.properties", false)));
    }

    private static final class Holder {
        private static final FrameworkConfig INSTANCE = load();

        private static FrameworkConfig load() {
            String env = activeEnv();
            FrameworkConfig config = new ConfigLoader(env, layersFor(env)).load();
            LOG.info("Configuration loaded: {}", config.summary());
            return config;
        }
    }
}
