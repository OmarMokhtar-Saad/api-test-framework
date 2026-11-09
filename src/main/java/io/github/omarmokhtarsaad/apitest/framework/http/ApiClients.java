package io.github.omarmokhtarsaad.apitest.framework.http;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;

/** The run's default {@link ApiClient}, built lazily from {@link ConfigProvider#get()}. */
public final class ApiClients {

    private ApiClients() {}

    /** One immutable client per JVM, shared safely by all parallel tests. */
    public static ApiClient defaultClient() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final ApiClient INSTANCE = ApiClient.from(ConfigProvider.get());
    }
}
