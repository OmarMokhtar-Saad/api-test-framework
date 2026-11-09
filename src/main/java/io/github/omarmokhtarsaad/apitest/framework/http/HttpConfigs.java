package io.github.omarmokhtarsaad.apitest.framework.http;

import io.github.omarmokhtarsaad.apitest.framework.json.ObjectMappers;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import java.time.Duration;

/** Builds per-client REST Assured configuration. Never touches the global {@code RestAssured} state. */
public final class HttpConfigs {

    private HttpConfigs() {}

    /**
     * Timeouts plus the shared Jackson mapper.
     *
     * @param connect time allowed to open the TCP connection
     * @param read maximum silence between data packets once connected
     */
    public static RestAssuredConfig restAssuredConfig(Duration connect, Duration read) {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .connectTimeout(connect)
                        .readTimeout(read))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> ObjectMappers.shared()));
    }
}
