package io.github.omarmokhtarsaad.apitest.framework.config;

/**
 * Every configuration key the framework reads. This is the single source of truth for key names;
 * docs/CONFIGURATION.md documents each one. Never use a key string literal elsewhere.
 */
public final class ConfigKeys {

    public static final String ENV = "env";
    public static final String BASE_URL = "baseUrl";
    public static final String BASE_PATH = "basePath";
    public static final String CONNECT_TIMEOUT_MS = "timeout.connect.ms";
    public static final String READ_TIMEOUT_MS = "timeout.read.ms";

    public static final String AUTH_TYPE = "auth.type";
    public static final String AUTH_TOKEN = "auth.token";
    public static final String AUTH_APIKEY_HEADER = "auth.apikey.header";
    public static final String AUTH_APIKEY_VALUE = "auth.apikey.value";
    public static final String AUTH_BASIC_USERNAME = "auth.basic.username";
    public static final String AUTH_BASIC_PASSWORD = "auth.basic.password";
    public static final String AUTH_OAUTH2_TOKEN_URL = "auth.oauth2.tokenUrl";
    public static final String AUTH_OAUTH2_CLIENT_ID = "auth.oauth2.clientId";
    public static final String AUTH_OAUTH2_CLIENT_SECRET = "auth.oauth2.clientSecret";
    public static final String AUTH_OAUTH2_SCOPE = "auth.oauth2.scope";

    public static final String HTTP_LOG = "http.log";
    public static final String HTTP_LOG_MAX_BODY_CHARS = "http.log.maxBodyChars";
    public static final String REDACT_EXTRA_KEYS = "redact.extraKeys";
    public static final String RETRY_MAX = "retry.max";
    public static final String DATA_SEED = "data.seed";
    public static final String THREADS = "threads";

    private ConfigKeys() {}
}
