package io.github.omarmokhtarsaad.apitest.framework.testng;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;
import io.github.omarmokhtarsaad.apitest.framework.config.HttpLogPolicy;
import io.github.omarmokhtarsaad.apitest.framework.logging.ExchangeLog;
import io.github.omarmokhtarsaad.apitest.framework.logging.HttpExchange;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Publishes the redacted HTTP exchanges of each test method to Allure and the log.
 *
 * <p>With {@code http.log=FAILURES} (the default) only failed or skipped tests publish; with
 * {@code ALL} every test does. The per-thread {@link ExchangeLog} is cleared before and after every
 * invocation so exchanges never leak between tests that share a thread.
 */
public final class HttpReportingListener implements IInvokedMethodListener {

    private static final Logger LOG = LoggerFactory.getLogger(HttpReportingListener.class);

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod()) {
            ExchangeLog.clear();
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        try {
            List<HttpExchange> exchanges = ExchangeLog.snapshot();
            if (!exchanges.isEmpty() && shouldPublish(ConfigProvider.get().httpLog(), result)) {
                publish(result, exchanges);
            }
        } finally {
            ExchangeLog.clear();
        }
    }

    static boolean shouldPublish(HttpLogPolicy policy, ITestResult result) {
        return switch (policy) {
            case ALL -> true;
            case FAILURES -> result.getStatus() != ITestResult.SUCCESS;
            case NONE -> false;
        };
    }

    private static void publish(ITestResult result, List<HttpExchange> exchanges) {
        boolean failed = result.getStatus() != ITestResult.SUCCESS;
        for (HttpExchange exchange : exchanges) {
            String rendered = exchange.render();
            Allure.attachment(exchange.title(), "text/plain", rendered, AttachmentOptions.withFileExtension(".txt"));
            if (failed) {
                LOG.warn("{} - HTTP exchange:\n{}", result.getMethod().getQualifiedName(), rendered);
            }
        }
    }
}
