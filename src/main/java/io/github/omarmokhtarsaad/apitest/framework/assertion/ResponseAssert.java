package io.github.omarmokhtarsaad.apitest.framework.assertion;

import io.github.omarmokhtarsaad.apitest.framework.json.ObjectMappers;
import io.github.omarmokhtarsaad.apitest.framework.logging.ExchangeLog;
import io.github.omarmokhtarsaad.apitest.framework.logging.HttpExchange;
import io.github.omarmokhtarsaad.apitest.framework.logging.Redactor;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/**
 * Fluent AssertJ assertions for a REST Assured {@link Response}.
 *
 * <p>Every failure message ends with the redacted HTTP exchange that produced the response, so a
 * red test explains itself without re-running it.
 */
public final class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {

    static final int MAX_BODY_CHARS_IN_MESSAGE = 2_000;

    /** Null in the default fail-fast mode; the collected failure messages in soft mode. */
    private final List<String> softFailures;

    ResponseAssert(Response actual) {
        this(actual, null);
    }

    ResponseAssert(Response actual, List<String> softFailures) {
        super(actual, ResponseAssert.class);
        this.softFailures = softFailures;
    }

    @Override
    protected void failWithMessage(String errorMessage, Object... arguments) {
        if (softFailures == null) {
            super.failWithMessage(errorMessage, arguments);
            return;
        }
        try {
            super.failWithMessage(errorMessage, arguments);
        } catch (AssertionError e) {
            softFailures.add(e.getMessage());
        }
    }

    /** Asserts the exact HTTP status code. */
    public ResponseAssert hasStatus(int expected) {
        isNotNull();
        int status = actual.statusCode();
        if (status != expected) {
            failWithContext("Expected HTTP status %d but was %d", expected, status);
        }
        return this;
    }

    /** Asserts a 2xx status code. */
    public ResponseAssert isSuccessful() {
        isNotNull();
        int status = actual.statusCode();
        if (status < 200 || status > 299) {
            failWithContext("Expected a 2xx status but was %d", status);
        }
        return this;
    }

    /** Asserts that the header is present, with any value. */
    public ResponseAssert hasHeader(String name) {
        isNotNull();
        if (!actual.headers().hasHeaderWithName(name)) {
            failWithContext("Expected header '%s' to be present", name);
        }
        return this;
    }

    /** Asserts that the header is present with exactly {@code value}. */
    public ResponseAssert hasHeader(String name, String value) {
        isNotNull();
        if (!actual.headers().hasHeaderWithName(name)) {
            return hasHeader(name);
        }
        String actualValue = actual.header(name);
        if (!Objects.equals(actualValue, value)) {
            failWithContext("Expected header '%s' to be '%s' but was '%s'", name, value, actualValue);
        }
        return this;
    }

    /** Asserts that the Content-Type starts with {@code expected}, ignoring parameters such as charset. */
    public ResponseAssert hasContentType(String expected) {
        isNotNull();
        String contentType = actual.contentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(expected.toLowerCase())) {
            failWithContext("Expected Content-Type '%s' but was '%s'", expected, contentType);
        }
        return this;
    }

    /**
     * Asserts that the body matches a JSON schema on the classpath.
     *
     * @param classpathSchema for example {@code schemas/city/cities.json}
     */
    public ResponseAssert matchesSchema(String classpathSchema) {
        isNotNull();
        Matcher<?> matcher = JsonSchemaValidator.matchesJsonSchemaInClasspath(classpathSchema);
        String body = actual.asString();
        if (!matcher.matches(body)) {
            StringDescription mismatch = new StringDescription();
            matcher.describeMismatch(body, mismatch);
            failWithContext("Body does not match schema '%s':%n%s", classpathSchema, mismatch);
        }
        return this;
    }

    /** Asserts that the server answered within {@code limit}. */
    public ResponseAssert respondedWithin(Duration limit) {
        isNotNull();
        long elapsedMs = actual.timeIn(TimeUnit.MILLISECONDS);
        if (elapsedMs > limit.toMillis()) {
            failWithContext("Expected a response within %d ms but it took %d ms", limit.toMillis(), elapsedMs);
        }
        return this;
    }

    /** Asserts that the JSON path resolves to a non-null value; keeps the chain on the response. */
    public ResponseAssert hasJsonPath(String path) {
        isNotNull();
        if (actual.jsonPath().get(path) == null) {
            failWithContext("Expected JSON path '%s' to be not null", path);
        }
        return this;
    }

    /** Asserts that the JSON path resolves to {@code expected}; keeps the chain on the response. */
    public ResponseAssert hasJsonPath(String path, Object expected) {
        isNotNull();
        Object value = actual.jsonPath().get(path);
        if (!Objects.equals(value, expected)) {
            failWithContext("Expected JSON path '%s' to be '%s' but was '%s'", path, expected, value);
        }
        return this;
    }

    /** Asserts that the JSON path is a list with exactly {@code expectedSize} elements. */
    public ResponseAssert hasJsonPathSize(String path, int expectedSize) {
        isNotNull();
        Collection<?> items = jsonPathCollection(path);
        if (items != null && items.size() != expectedSize) {
            failWithContext("Expected JSON path '%s' to have %d elements but had %d", path, expectedSize, items.size());
        }
        return this;
    }

    /** Asserts that the JSON path is a list containing {@code element}. */
    public ResponseAssert hasJsonPathContaining(String path, Object element) {
        isNotNull();
        Collection<?> items = jsonPathCollection(path);
        if (items != null && !items.contains(element)) {
            failWithContext("Expected JSON path '%s' to contain '%s' but was %s", path, element, items);
        }
        return this;
    }

    private Collection<?> jsonPathCollection(String path) {
        Object value = actual.jsonPath().get(path);
        if (value instanceof Collection<?> items) {
            return items;
        }
        failWithContext("Expected JSON path '%s' to be a list but was '%s'", path, value);
        return null;
    }

    /** Deserialises the body, runs {@code requirements} on it and keeps the chain on the response. */
    public <T> ResponseAssert hasBody(Class<T> type, Consumer<? super T> requirements) {
        isNotNull();
        T value = readBody(type);
        if (value == null) {
            return this;
        }
        try {
            requirements.accept(value);
        } catch (AssertionError e) {
            if (softFailures == null) {
                throw e;
            }
            softFailures.add(e.getMessage());
        }
        return this;
    }

    /** Navigates to a JSON path (REST Assured GPath syntax) and returns an assert on the value. */
    public ObjectAssert<Object> jsonPath(String path) {
        isNotNull();
        Object value = actual.jsonPath().get(path);
        return Assertions.assertThat(value).as("JSON path '%s'%n%s", path, exchangeContext());
    }

    /** Deserialises the body with the shared mapper and returns an assert on the result. */
    public <T> ObjectAssert<T> bodyAs(Class<T> type) {
        isNotNull();
        return Assertions.assertThat(readBody(type)).as("body as %s", type.getSimpleName());
    }

    /** The deserialised body; null only in soft mode after a recorded failure. */
    private <T> T readBody(Class<T> type) {
        try {
            return ObjectMappers.shared().readValue(actual.asByteArray(), type);
        } catch (IOException e) {
            failWithContext("Body is not a valid %s: %s", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private void failWithContext(String format, Object... args) {
        if (softFailures != null) {
            failWithMessage("%s", format.formatted(args));
            return;
        }
        failWithMessage("%s%n%s", format.formatted(args), exchangeContext());
    }

    /**
     * The recorded exchange for the current thread if there is one; otherwise the body, redacted and
     * truncated. Never returns unredacted data.
     */
    String exchangeContext() {
        return ExchangeLog.last()
                .map(HttpExchange::render)
                .orElseGet(() ->
                        "Response body (redacted):%n%s".formatted(new Redactor().redact(truncate(actual.asString()))));
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= MAX_BODY_CHARS_IN_MESSAGE
                ? body
                : body.substring(0, MAX_BODY_CHARS_IN_MESSAGE) + "... [truncated]";
    }
}
