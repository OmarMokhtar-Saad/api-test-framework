package io.github.omarmokhtarsaad.apitest.framework.testng;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;
import io.github.omarmokhtarsaad.apitest.framework.config.FrameworkConfig;
import io.github.omarmokhtarsaad.apitest.framework.data.TestDataFactory;
import io.github.omarmokhtarsaad.apitest.framework.http.ApiClient;
import io.github.omarmokhtarsaad.apitest.framework.http.ApiClients;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;

/**
 * Optional base class for API tests. It holds no per-test state in fields, so test classes stay
 * safe under {@code parallel="methods"}.
 *
 * <p>Register cleanup right after creating data: {@code onCleanup("delete user " + id, () ->
 * users.delete(id))}. Cleanups run after each test in reverse order, even when the test failed.
 */
public abstract class BaseApiTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseApiTest.class);

    private static final ThreadLocal<Deque<Cleanup>> CLEANUPS = ThreadLocal.withInitial(ArrayDeque::new);

    /** The shared, immutable client configured for the active environment. */
    protected ApiClient api() {
        return ApiClients.defaultClient();
    }

    protected FrameworkConfig config() {
        return ConfigProvider.get();
    }

    /** Seeded test data for the current thread. */
    protected TestDataFactory data() {
        return TestDataFactory.current();
    }

    /** Schedules {@code action} to run after the current test method. */
    protected void onCleanup(String description, Runnable action) {
        CLEANUPS.get().push(new Cleanup(Objects.requireNonNull(description), Objects.requireNonNull(action)));
    }

    /**
     * Runs the registered cleanups. A failing cleanup is logged and attached but not rethrown: a
     * throwing {@code @AfterMethod} would make TestNG skip every later test in the class.
     */
    @AfterMethod(alwaysRun = true)
    public final void runCleanups() {
        Deque<Cleanup> cleanups = CLEANUPS.get();
        try {
            while (!cleanups.isEmpty()) {
                Cleanup cleanup = cleanups.pop();
                try {
                    cleanup.action().run();
                } catch (RuntimeException e) {
                    LOG.error("Cleanup '{}' failed", cleanup.description(), e);
                    Allure.attachment(
                            "Cleanup failed: " + cleanup.description(),
                            "text/plain",
                            stackTrace(e),
                            AttachmentOptions.withFileExtension(".txt"));
                }
            }
        } finally {
            CLEANUPS.remove();
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }

    private record Cleanup(String description, Runnable action) {}
}
