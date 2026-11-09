package io.github.omarmokhtarsaad.apitest.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.omarmokhtarsaad.apitest.framework.auth.AccessToken;
import io.github.omarmokhtarsaad.apitest.framework.auth.AuthenticationException;
import io.github.omarmokhtarsaad.apitest.framework.auth.ClientCredentialsTokenProvider;
import io.github.omarmokhtarsaad.apitest.framework.config.AuthSettings;
import io.github.omarmokhtarsaad.apitest.framework.config.Secret;
import io.github.omarmokhtarsaad.apitest.framework.http.HttpConfigs;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Runs against its own WireMock server, independent of the mock environment's server. */
@Test(groups = "unit")
public class ClientCredentialsTokenProviderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private WireMockServer server;

    @BeforeClass(alwaysRun = true)
    public void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        server.stubFor(post(urlEqualTo("/ok"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=demo"))
                .withRequestBody(containing("client_secret=top-secret"))
                .withRequestBody(containing("scope=read"))
                .willReturn(json(200, "{\"access_token\":\"abc\",\"token_type\":\"Bearer\",\"expires_in\":300}")));
        server.stubFor(post(urlEqualTo("/no-expiry")).willReturn(json(200, "{\"access_token\":\"abc\"}")));
        server.stubFor(post(urlEqualTo("/denied")).willReturn(json(401, "{\"error\":\"invalid_client\"}")));
        server.stubFor(post(urlEqualTo("/empty")).willReturn(json(200, "{\"token_type\":\"Bearer\"}")));
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private ClientCredentialsTokenProvider provider(String path, Optional<String> scope) {
        AuthSettings.OAuth2ClientCredentials settings = new AuthSettings.OAuth2ClientCredentials(
                URI.create(server.baseUrl() + path), "demo", Secret.of("top-secret"), scope);
        return new ClientCredentialsTokenProvider(
                settings,
                HttpConfigs.restAssuredConfig(Duration.ofSeconds(2), Duration.ofSeconds(2)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    public void fetchesTokenAndComputesExpiry() {
        AccessToken token = provider("/ok", Optional.of("read")).token();

        assertThat(token.value().reveal()).isEqualTo("abc");
        assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(300));
    }

    public void tokenWithoutExpiryNeverExpires() {
        assertThat(provider("/no-expiry", Optional.empty()).token().expiresAt()).isEqualTo(Instant.MAX);
    }

    public void errorStatusReportsCodeButNotSecret() {
        assertThatThrownBy(() -> provider("/denied", Optional.empty()).token())
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageContaining("invalid_client")
                .hasMessageNotContaining("top-secret");
    }

    public void missingAccessTokenIsAnError() {
        assertThatThrownBy(() -> provider("/empty", Optional.empty()).token())
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("no access_token");
    }
}
