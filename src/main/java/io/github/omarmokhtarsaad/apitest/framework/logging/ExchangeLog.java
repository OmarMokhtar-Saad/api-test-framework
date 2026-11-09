package io.github.omarmokhtarsaad.apitest.framework.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The HTTP exchanges made by the test running on the current thread.
 *
 * <p>Thread-confined: with {@code parallel="methods"} each test method runs on one worker thread, so
 * a thread-local log keeps tests from seeing each other's traffic. {@code HttpReportingListener}
 * publishes it after each test and always calls {@link #clear()}, so nothing leaks into the next
 * test that reuses the thread.
 */
public final class ExchangeLog {

    /** Upper bound per test, so a polling loop cannot exhaust memory. Oldest entries are dropped. */
    static final int MAX_ENTRIES = 100;

    private static final ThreadLocal<List<HttpExchange>> EXCHANGES = ThreadLocal.withInitial(ArrayList::new);

    private ExchangeLog() {}

    public static void add(HttpExchange exchange) {
        List<HttpExchange> list = EXCHANGES.get();
        if (list.size() >= MAX_ENTRIES) {
            list.removeFirst();
        }
        list.add(exchange);
    }

    /** An immutable copy of this thread's exchanges, oldest first. */
    public static List<HttpExchange> snapshot() {
        return List.copyOf(EXCHANGES.get());
    }

    /** The most recent exchange on this thread. */
    public static Optional<HttpExchange> last() {
        List<HttpExchange> list = EXCHANGES.get();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getLast());
    }

    public static void clear() {
        EXCHANGES.remove();
    }
}
