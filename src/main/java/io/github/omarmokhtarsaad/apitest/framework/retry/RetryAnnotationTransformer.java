package io.github.omarmokhtarsaad.apitest.framework.retry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

/**
 * Installs {@link TransientFailureRetryAnalyzer} on every {@code @Test} that does not declare its own
 * analyzer. Registered through {@code META-INF/services/org.testng.ITestNGListener}.
 */
public final class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    @SuppressWarnings("rawtypes") // signature fixed by TestNG
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        Class<?> current = annotation.getRetryAnalyzerClass();
        if (current == null || current == DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(TransientFailureRetryAnalyzer.class);
        }
    }
}
