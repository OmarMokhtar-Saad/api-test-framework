package io.github.omarmokhtarsaad.apitest.framework.retry;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;
import java.io.IOException;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a test only when it failed on a transport problem (an {@link IOException} somewhere in the
 * cause chain: connection refused, reset, read timeout).
 *
 * <p>Assertion failures and HTTP error statuses are never retried: they are real results, and
 * retrying them would hide defects. TestNG creates one instance per test method (and per data
 * provider row), so the counter is per method.
 */
public final class TransientFailureRetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(TransientFailureRetryAnalyzer.class);

    private final IntSupplier maxRetries;
    private int attempts;

    /**
     * Used by TestNG. Reads {@code retry.max} on the first transient failure, not at construction, so
     * the configuration is never loaded before suite listeners have run.
     */
    public TransientFailureRetryAnalyzer() {
        this.maxRetries = () -> ConfigProvider.get().retryMax();
    }

    public TransientFailureRetryAnalyzer(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0, was " + maxRetries);
        }
        this.maxRetries = () -> maxRetries;
    }

    @Override
    public boolean retry(ITestResult result) {
        if (!isTransient(result.getThrowable())) {
            return false;
        }
        int max = maxRetries.getAsInt();
        if (attempts >= max) {
            return false;
        }
        attempts++;
        LOG.warn(
                "Retrying {} after a transient failure (attempt {}/{}): {}",
                result.getMethod().getQualifiedName(),
                attempts,
                max,
                result.getThrowable().toString());
        return true;
    }

    static boolean isTransient(Throwable failure) {
        int depth = 0;
        for (Throwable t = failure; t != null && depth < 50; t = t.getCause(), depth++) {
            if (t instanceof IOException) {
                return true;
            }
        }
        return false;
    }
}
