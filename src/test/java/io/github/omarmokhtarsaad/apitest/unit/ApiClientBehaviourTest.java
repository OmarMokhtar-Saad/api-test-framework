package io.github.omarmokhtarsaad.apitest.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.github.omarmokhtarsaad.apitest.framework.assertion.ApiAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.omarmokhtarsaad.apitest.framework.auth.BearerTokenAuth;
import io.github.omarmokhtarsaad.apitest.framework.auth.StaticTokenProvider;
import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import io.github.omarmokhtarsaad.apitest.framework.http.ApiClient;
import io.github.omarmokhtarsaad.apitest.framework.http.HttpConfigs;
import io.github.omarmokhtarsaad.apitest.framework.logging.ExchangeLog;
import io.github.omarmokhtarsaad.apitest.framework.logging.HttpExchangeRecorder;
import io.github.omarmokhtarsaad.apitest.framework.logging.Redactor;
import io.restassured.response.Response;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * {@link ApiClient} wiring and {@code ResponseAssert} messages, against a private WireMock server.
 * Builds clients by hand, so it never loads the run's configuration.
 */
@Test(groups = "unit")
public class ApiClientBehaviourTest {

    private static final String TOKEN = "unit-secret-token-123";

    private WireMockServer server;

    @BeforeClass(alwaysRun = true)
    public void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        server.stubFor(get(urlEqualTo("/echo-token"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"" + TOKEN + "\",\"message\":\"boom\"}")));
        server.stubFor(get(urlEqualTo("/slow"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(2_000).withBody("{}")));
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    private ApiClient client(Duration readTimeout, boolean record) {
        return new ApiClient(
                URI.create(server.baseUrl()),
                "",
                HttpConfigs.restAssuredConfig(Duration.ofSeconds(2), readTimeout),
                new BearerTokenAuth(new StaticTokenProvider(Secret.of(TOKEN))),
                record ? List.of(new HttpExchangeRecorder(new Redactor(), 10_000)) : List.of());
    }

    public void failureMessageCarriesRedactedExchange() {
        try {
            Response response = client(Duration.ofSeconds(5), true).request().get("/echo-token");

            assertThatThrownBy(() -> assertThat(response).hasStatus(200))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected HTTP status 200 but was 500")
                    .hasMessageContaining("GET")
                    .hasMessageContaining("[REDACTED]")
                    .hasMessageContaining("boom")
                    .hasMessageNotContaining(TOKEN);
        } finally {
            // Leave nothing for the reporting listener, so this test never loads the run's configuration.
            ExchangeLog.clear();
        }
    }

    public void failureMessageWithoutRecorderFallsBackToRedactedBody() {
        ExchangeLog.clear();
        Response response = client(Duration.ofSeconds(5), false).request().get("/echo-token");

        assertThatThrownBy(() -> assertThat(response).isSuccessful())
                .hasMessageContaining("Response body (redacted)")
                .hasMessageContaining("[REDACTED]")
                .hasMessageNotContaining(TOKEN);
    }

    public void readTimeoutIsEnforced() {
        ApiClient impatient = client(Duration.ofMillis(300), false);

        Throwable thrown = catchThrowable(() -> impatient.request().get("/slow"));

        // REST Assured rethrows the socket exception as-is, but a future version may wrap it.
        Assertions.assertThat(rootCauseOf(thrown)).isInstanceOf(SocketTimeoutException.class);
    }

    private static Throwable rootCauseOf(Throwable thrown) {
        Throwable current = thrown;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
