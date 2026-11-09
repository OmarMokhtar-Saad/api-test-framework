package io.github.omarmokhtarsaad.apitest.framework.logging;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Records every request and response into {@link ExchangeLog}, redacted and truncated at capture
 * time so raw secrets are never held in memory longer than the request itself.
 *
 * <p>Runs last among filters, so it records the request exactly as sent (including the headers the
 * auth filter added - which the {@link Redactor} then masks). Transport errors are recorded too and
 * rethrown unchanged.
 */
public final class HttpExchangeRecorder implements OrderedFilter {

    private final Redactor redactor;
    private final int maxBodyChars;

    public HttpExchangeRecorder(Redactor redactor, int maxBodyChars) {
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        if (maxBodyChars <= 0) {
            throw new IllegalArgumentException("maxBodyChars must be > 0");
        }
        this.maxBodyChars = maxBodyChars;
    }

    @Override
    public Response filter(
            FilterableRequestSpecification request, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        String renderedRequest = redactor.redact(renderRequest(request));
        long start = System.nanoTime();
        try {
            Response response = ctx.next(request, responseSpec);
            ExchangeLog.add(new HttpExchange(
                    request.getMethod(),
                    redactor.redact(request.getURI()),
                    Optional.of(response.statusCode()),
                    elapsedSince(start),
                    renderedRequest,
                    redactor.redact(renderResponse(response)),
                    Optional.empty()));
            return response;
        } catch (Exception e) {
            ExchangeLog.add(new HttpExchange(
                    request.getMethod(),
                    redactor.redact(request.getURI()),
                    Optional.empty(),
                    elapsedSince(start),
                    renderedRequest,
                    "",
                    Optional.of(redactor.redact(e.toString()))));
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private String renderRequest(FilterableRequestSpecification request) {
        StringBuilder text = new StringBuilder()
                .append(request.getMethod())
                .append(' ')
                .append(request.getURI())
                .append('\n');
        for (Header header : request.getHeaders()) {
            text.append(header.getName()).append(": ").append(header.getValue()).append('\n');
        }
        Map<String, String> form = request.getFormParams();
        if (!form.isEmpty()) {
            text.append('\n');
            form.forEach((k, v) -> text.append(k).append('=').append(v).append('\n'));
        }
        Object body = request.getBody();
        if (body != null) {
            text.append('\n').append(truncate(String.valueOf(body)));
        }
        return text.toString();
    }

    private String renderResponse(Response response) {
        StringBuilder text = new StringBuilder(response.getStatusLine()).append('\n');
        for (Header header : response.getHeaders()) {
            text.append(header.getName()).append(": ").append(header.getValue()).append('\n');
        }
        String body = response.asString();
        if (body != null && !body.isEmpty()) {
            text.append('\n').append(truncate(body));
        }
        return text.toString();
    }

    private String truncate(String text) {
        if (text.length() <= maxBodyChars) {
            return text;
        }
        return text.substring(0, maxBodyChars) + "\n... [truncated " + (text.length() - maxBodyChars) + " chars]";
    }

    private static Duration elapsedSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
