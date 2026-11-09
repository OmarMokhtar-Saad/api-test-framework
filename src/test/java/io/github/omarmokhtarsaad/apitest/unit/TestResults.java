package io.github.omarmokhtarsaad.apitest.unit;

import java.lang.reflect.Proxy;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

/** Minimal {@link ITestResult} stand-ins built with dynamic proxies, so unit tests need no mocking library. */
final class TestResults {

    private TestResults() {}

    static ITestResult failedWith(Throwable failure) {
        ITestNGMethod method = (ITestNGMethod) Proxy.newProxyInstance(
                TestResults.class.getClassLoader(),
                new Class<?>[] {ITestNGMethod.class},
                (proxy, m, args) -> switch (m.getName()) {
                    case "getQualifiedName" -> "unit.FakeTest.method";
                    case "toString" -> "FakeMethod";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
        return (ITestResult) Proxy.newProxyInstance(
                TestResults.class.getClassLoader(),
                new Class<?>[] {ITestResult.class},
                (proxy, m, args) -> switch (m.getName()) {
                    case "getStatus" -> ITestResult.FAILURE;
                    case "getThrowable" -> failure;
                    case "getMethod" -> method;
                    case "toString" -> "FakeResult";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }
}
