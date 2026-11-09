package io.github.omarmokhtarsaad.apitest.framework.config;

/** Thrown when configuration is missing, malformed, or supplied through an unsafe channel. */
public final class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
