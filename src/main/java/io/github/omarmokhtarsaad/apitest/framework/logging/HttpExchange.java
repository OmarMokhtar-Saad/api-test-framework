package io.github.omarmokhtarsaad.apitest.framework.logging;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * One captured request/response pair. {@code request} and {@code response} are rendered text that
 * has already been redacted and truncated, so an {@code HttpExchange} is always safe to publish.
 *
 * @param status the HTTP status, or empty if no response arrived (connection error, timeout)
 * @param error the transport error, if any
 */
public record HttpExchange(
        String method,
        String url,
        Optional<Integer> status,
        Duration elapsed,
        String request,
        String response,
        Optional<String> error) {

    public HttpExchange {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(error, "error");
    }

    /** {@code GET https://host/path -> 200 (35 ms)}. */
    public String title() {
        String outcome = status.map(String::valueOf).orElse("no response");
        return "%s %s -> %s (%d ms)".formatted(method, url, outcome, elapsed.toMillis());
    }

    /** Full, human-readable form used in reports and assertion messages. */
    public String render() {
        StringBuilder text = new StringBuilder()
                .append(title())
                .append("\n\n--- Request ---\n")
                .append(request)
                .append("\n\n--- Response ---\n")
                .append(response);
        error.ifPresent(e -> text.append("\n\n--- Error ---\n").append(e));
        return text.toString();
    }
}
