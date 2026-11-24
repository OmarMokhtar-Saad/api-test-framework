package io.github.omarmokhtarsaad.apitest.framework.assertion;

import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;

/**
 * Single assertion entry point: {@code import static ...ApiAssertions.assertThat;} gives the HTTP
 * assertions below plus every standard AssertJ {@code assertThat} overload.
 */
public final class ApiAssertions extends Assertions {

    private ApiAssertions() {}

    public static ResponseAssert assertThat(Response response) {
        return new ResponseAssert(response);
    }

    /**
     * Runs every assertion in {@code checks} even when one fails, then throws a single error that
     * lists all failures followed by the HTTP exchange once.
     */
    public static void assertSoftly(Response response, Consumer<ResponseAssert> checks) {
        List<String> failures = new ArrayList<>();
        ResponseAssert soft = new ResponseAssert(response, failures);
        try {
            checks.accept(soft);
        } catch (AssertionError e) {
            failures.add(e.getMessage());
        }
        if (failures.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder().append(failures.size()).append(" assertion failure(s):");
        for (int i = 0; i < failures.size(); i++) {
            message.append(System.lineSeparator()).append(i + 1).append(") ").append(failures.get(i));
        }
        message.append(System.lineSeparator()).append(soft.exchangeContext());
        throw new AssertionError(message.toString());
    }
}
