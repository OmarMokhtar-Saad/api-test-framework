package io.github.omarmokhtarsaad.apitest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.omarmokhtarsaad.apitest.framework.retry.TransientFailureRetryAnalyzer;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TransientFailureRetryAnalyzerTest {

    public void retriesTransportFailuresUpToTheLimit() {
        TransientFailureRetryAnalyzer analyzer = new TransientFailureRetryAnalyzer(2);
        RuntimeException failure =
                new RuntimeException("wrapped", new UncheckedIOException(new SocketTimeoutException("Read timed out")));

        assertThat(analyzer.retry(TestResults.failedWith(failure))).isTrue();
        assertThat(analyzer.retry(TestResults.failedWith(failure))).isTrue();
        assertThat(analyzer.retry(TestResults.failedWith(failure))).isFalse();
    }

    public void neverRetriesAssertionFailures() {
        TransientFailureRetryAnalyzer analyzer = new TransientFailureRetryAnalyzer(5);

        assertThat(analyzer.retry(TestResults.failedWith(new AssertionError("Expected HTTP status 200 but was 500"))))
                .isFalse();
    }

    public void zeroDisablesRetries() {
        assertThat(new TransientFailureRetryAnalyzer(0).retry(TestResults.failedWith(new SocketTimeoutException())))
                .isFalse();
    }

    public void survivesCyclicCauseChains() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        assertThat(new TransientFailureRetryAnalyzer(1).retry(TestResults.failedWith(a)))
                .isFalse();
    }

    public void rejectsNegativeLimit() {
        assertThatThrownBy(() -> new TransientFailureRetryAnalyzer(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
