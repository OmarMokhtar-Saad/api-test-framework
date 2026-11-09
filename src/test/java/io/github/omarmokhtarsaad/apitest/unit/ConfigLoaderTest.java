package io.github.omarmokhtarsaad.apitest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.omarmokhtarsaad.apitest.framework.config.AuthSettings;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigLoader;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigurationException;
import io.github.omarmokhtarsaad.apitest.framework.config.EnvironmentVariableSource;
import io.github.omarmokhtarsaad.apitest.framework.config.FrameworkConfig;
import io.github.omarmokhtarsaad.apitest.framework.config.HttpLogPolicy;
import io.github.omarmokhtarsaad.apitest.framework.config.LayeredConfig;
import io.github.omarmokhtarsaad.apitest.framework.config.PropertiesSource;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Pure in-memory layers: no system properties, files or network, so it never sees the mock env. */
@Test(groups = "unit")
public class ConfigLoaderTest {

    /** A committed (not secret-safe) file layer below API_* environment variables (secret-safe). */
    private static FrameworkConfig load(Map<String, String> file, Map<String, String> env) {
        Properties p = new Properties();
        p.putAll(file);
        LayeredConfig layers = new LayeredConfig(
                List.of(new EnvironmentVariableSource(env), new PropertiesSource("file test.properties", p, false)));
        return new ConfigLoader("test", layers).load();
    }

    private static Map<String, String> minimal() {
        Map<String, String> m = new HashMap<>();
        m.put("baseUrl", "https://api.example.test");
        return m;
    }

    public void appliesDefaults() {
        FrameworkConfig config = load(minimal(), Map.of());

        assertThat(config.env()).isEqualTo("test");
        assertThat(config.basePath()).isEmpty();
        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(config.auth()).isInstanceOf(AuthSettings.None.class);
        assertThat(config.httpLog()).isEqualTo(HttpLogPolicy.FAILURES);
        assertThat(config.retryMax()).isEqualTo(1);
        assertThat(config.dataSeed()).isPositive();
    }

    public void normalisesAndParsesValues() {
        Map<String, String> file = minimal();
        file.put("basePath", "api/v1//");
        file.put("http.log", "all");
        file.put("redact.extraKeys", " NationalId , ,accountNumber");
        file.put("data.seed", "42");

        FrameworkConfig config = load(file, Map.of());

        assertThat(config.basePath()).isEqualTo("/api/v1");
        assertThat(config.httpLog()).isEqualTo(HttpLogPolicy.ALL);
        assertThat(config.extraRedactKeys()).containsExactlyInAnyOrder("nationalid", "accountnumber");
        assertThat(config.dataSeed()).isEqualTo(42L);
    }

    public void environmentVariableOverridesFile() {
        FrameworkConfig config = load(minimal(), Map.of("API_BASE_URL", "http://override.example.test:8080"));

        assertThat(config.baseUrl()).hasToString("http://override.example.test:8080");
    }

    public void missingBaseUrlNamesEveryPlaceToSetIt() {
        assertThatThrownBy(() -> load(Map.of(), Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("-DbaseUrl")
                .hasMessageContaining("API_BASE_URL")
                .hasMessageContaining("env/test.properties");
    }

    @DataProvider
    public Object[][] invalidValues() {
        return new Object[][] {
            {"baseUrl", "ftp://api.example.test", "absolute http(s) URL"},
            {"baseUrl", "not a url", "valid URI"},
            {"baseUrl", "https://user:pass@api.example.test", "must not embed credentials"},
            {"timeout.read.ms", "0", "greater than zero"},
            {"timeout.read.ms", "abc", "must be an integer"},
            {"retry.max", "-1", "zero or more"},
            {"http.log", "verbose", "must be one of"},
            {"auth.type", "kerberos", "must be one of none, bearer"},
        };
    }

    @Test(dataProvider = "invalidValues")
    public void rejectsInvalidValues(String key, String value, String expectedMessage) {
        Map<String, String> file = minimal();
        file.put(key, value);

        assertThatThrownBy(() -> load(file, Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining(key)
                .hasMessageContaining(expectedMessage);
    }

    public void credentialsInUrlAreNotEchoedBack() {
        Map<String, String> file = minimal();
        file.put("baseUrl", "https://user:hunter2@api.example.test");

        assertThatThrownBy(() -> load(file, Map.of())).hasMessageNotContaining("hunter2");
    }

    @DataProvider
    public Object[][] invalidEnvNames() {
        return new Object[][] {{null}, {""}, {"QA"}, {"../secrets"}, {"qa/prod"}, {"-qa"}};
    }

    @Test(dataProvider = "invalidEnvNames")
    public void rejectsInvalidEnvironmentNames(String env) {
        assertThatThrownBy(() -> ConfigLoader.validateEnvName(env)).isInstanceOf(ConfigurationException.class);
    }

    public void oauth2SecretComesFromSecretSafeLayer() {
        Map<String, String> file = minimal();
        file.put("auth.type", "oauth2");
        file.put("auth.oauth2.tokenUrl", "https://auth.example.test/token");
        file.put("auth.oauth2.clientId", "demo");

        FrameworkConfig config = load(file, Map.of("API_AUTH_OAUTH2_CLIENT_SECRET", "env-secret"));

        assertThat(config.auth()).isInstanceOfSatisfying(AuthSettings.OAuth2ClientCredentials.class, oauth -> {
            assertThat(oauth.clientId()).isEqualTo("demo");
            assertThat(oauth.clientSecret().reveal()).isEqualTo("env-secret");
            assertThat(oauth.scope()).isEmpty();
        });
        assertThat(config.toString()).doesNotContain("env-secret");
        assertThat(config.summary()).doesNotContain("env-secret");
    }

    public void secretInCommittedFileIsRejected() {
        Map<String, String> file = minimal();
        file.put("auth.type", "bearer");
        file.put("auth.token", "committed-token");

        assertThatThrownBy(() -> load(file, Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("not allowed")
                .hasMessageNotContaining("committed-token");
    }

    public void missingSecretPointsToSecretChannelsOnly() {
        Map<String, String> file = minimal();
        file.put("auth.type", "bearer");

        assertThatThrownBy(() -> load(file, Map.of()))
                .hasMessageContaining("API_AUTH_TOKEN")
                .hasMessageContaining("test.secrets.properties")
                .hasMessageNotContaining("-Dauth.token");
    }
}
