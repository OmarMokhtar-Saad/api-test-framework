package io.github.omarmokhtarsaad.apitest.support;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigKeys;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * For {@code env=mock} (the default), starts a WireMock server that plays the example Users API from
 * {@code src/test/resources/wiremock/} and points {@code baseUrl} and the token URL at it.
 *
 * <p>It runs before any test touches {@link ConfigProvider#get()}, so the loaded configuration sees
 * the dynamic port. One server per JVM, stopped at shutdown. For any other environment it does
 * nothing and the tests hit the real API.
 */
public final class MockApiServerListener implements ISuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger(MockApiServerListener.class);

    private static WireMockServer server;

    @Override
    public void onStart(ISuite suite) {
        if (ConfigProvider.DEFAULT_ENV.equals(ConfigProvider.activeEnv())) {
            ensureStarted();
        }
    }

    private static synchronized void ensureStarted() {
        if (server != null) {
            return;
        }
        WireMockServer started = new WireMockServer(options().dynamicPort().usingFilesUnderClasspath("wiremock"));
        started.start();
        Runtime.getRuntime().addShutdownHook(new Thread(started::stop, "wiremock-stop"));
        server = started;

        String baseUrl = "http://localhost:" + started.port();
        System.setProperty(ConfigKeys.BASE_URL, baseUrl);
        System.setProperty(ConfigKeys.AUTH_OAUTH2_TOKEN_URL, baseUrl + "/oauth/token");
        LOG.info("Mock API server listening on {}", baseUrl);
    }
}
