package io.github.omarmokhtarsaad.apitest.framework.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The one Jackson configuration used for every request and response body.
 *
 * <ul>
 *   <li>Unknown response fields are ignored: an API adding a field must not break unrelated tests.
 *       Contract tests (JSON Schema) are where unexpected shape changes are caught.
 *   <li>{@code java.time} types are written as ISO-8601 strings, not epoch numbers.
 *   <li>{@code null} fields are omitted from request bodies, so "not set" and "explicitly null" are
 *       distinguishable in negative tests (send a {@code Map} to force an explicit null).
 * </ul>
 */
public final class ObjectMappers {

    private static final ObjectMapper SHARED = create();

    private ObjectMappers() {}

    /** The shared, fully configured mapper. Thread-safe; never reconfigure it. */
    public static ObjectMapper shared() {
        return SHARED;
    }

    /** A new mapper with the framework defaults, for callers that need to customise further. */
    public static ObjectMapper create() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultPropertyInclusion(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                .build();
    }
}
