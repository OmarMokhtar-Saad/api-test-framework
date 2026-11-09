package io.github.omarmokhtarsaad.apitest.framework.testng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigurationException;
import io.github.omarmokhtarsaad.apitest.framework.config.HttpLogPolicy;
import java.lang.reflect.Proxy;
import org.testng.ITestResult;
import org.testng.annotations.Test;

/**
 * Unit test for the listeners' package-private rules; it lives in their package to reach them. The
 * unit suites list it by class name.
 */
@Test(groups = "unit")
public class ListenerRulesTest {

    private static ITestResult withStatus(int status) {
        return (ITestResult) Proxy.newProxyInstance(
                ListenerRulesTest.class.getClassLoader(),
                new Class<?>[] {ITestResult.class},
                (proxy, method, args) -> method.getName().equals("getStatus") ? status : null);
    }

    public void failuresPolicyPublishesOnlyNonSuccess() {
        assertThat(HttpReportingListener.shouldPublish(HttpLogPolicy.FAILURES, withStatus(ITestResult.SUCCESS)))
                .isFalse();
        assertThat(HttpReportingListener.shouldPublish(HttpLogPolicy.FAILURES, withStatus(ITestResult.FAILURE)))
                .isTrue();
        assertThat(HttpReportingListener.shouldPublish(HttpLogPolicy.FAILURES, withStatus(ITestResult.SKIP)))
                .isTrue();
    }

    public void allAndNonePoliciesIgnoreStatus() {
        assertThat(HttpReportingListener.shouldPublish(HttpLogPolicy.ALL, withStatus(ITestResult.SUCCESS)))
                .isTrue();
        assertThat(HttpReportingListener.shouldPublish(HttpLogPolicy.NONE, withStatus(ITestResult.FAILURE)))
                .isFalse();
    }

    public void parsesThreadCount() {
        assertThat(ParallelismListener.parse(" 8 ")).isEqualTo(8);
    }

    public void rejectsInvalidThreadCounts() {
        assertThatThrownBy(() -> ParallelismListener.parse("0")).isInstanceOf(ConfigurationException.class);
        assertThatThrownBy(() -> ParallelismListener.parse("many"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("many");
    }
}
