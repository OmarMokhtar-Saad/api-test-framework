package io.github.omarmokhtarsaad.apitest.framework.assertion;

import io.restassured.response.Response;
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
}
