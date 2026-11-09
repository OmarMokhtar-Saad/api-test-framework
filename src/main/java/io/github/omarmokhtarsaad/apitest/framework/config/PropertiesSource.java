package io.github.omarmokhtarsaad.apitest.framework.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** A {@code .properties} file (or an in-memory {@link Properties}) as a configuration layer. */
public final class PropertiesSource implements ConfigSource {

    private final String name;
    private final Properties properties;
    private final boolean secretSafe;

    public PropertiesSource(String name, Properties properties, boolean secretSafe) {
        this.name = Objects.requireNonNull(name, "name");
        this.properties = new Properties();
        this.properties.putAll(Objects.requireNonNull(properties, "properties"));
        this.secretSafe = secretSafe;
    }

    /**
     * Loads {@code resource} from the classpath. A missing resource yields an empty source: not
     * every environment needs every file.
     */
    public static PropertiesSource fromClasspath(String resource, boolean secretSafe) {
        Properties properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in != null) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new ConfigurationException("Could not read classpath resource '" + resource + "'", e);
        }
        return new PropertiesSource("file " + resource, properties, secretSafe);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    @Override
    public boolean secretSafe() {
        return secretSafe;
    }
}
