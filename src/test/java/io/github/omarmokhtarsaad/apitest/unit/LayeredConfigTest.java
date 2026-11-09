package io.github.omarmokhtarsaad.apitest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.omarmokhtarsaad.apitest.framework.config.ConfigSource;
import io.github.omarmokhtarsaad.apitest.framework.config.ConfigurationException;
import io.github.omarmokhtarsaad.apitest.framework.config.EnvironmentVariableSource;
import io.github.omarmokhtarsaad.apitest.framework.config.LayeredConfig;
import io.github.omarmokhtarsaad.apitest.framework.config.PropertiesSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LayeredConfigTest {

    private static PropertiesSource props(String name, boolean secretSafe, String... keyValues) {
        Properties p = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            p.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return new PropertiesSource(name, p, secretSafe);
    }

    public void firstSourceWithAValueWins() {
        LayeredConfig config = new LayeredConfig(
                List.of(props("high", false, "a", "high"), props("low", false, "a", "low", "b", "low-only")));

        assertThat(config.get("a")).contains("high");
        assertThat(config.get("b")).contains("low-only");
        assertThat(config.get("missing")).isEmpty();
    }

    public void blankValuesDoNotShadowLowerLayers() {
        LayeredConfig config =
                new LayeredConfig(List.of(props("high", false, "a", "   "), props("low", false, "a", " value ")));

        assertThat(config.get("a")).contains("value");
    }

    public void findReportsTheWinningSource() {
        ConfigSource low = props("low", false, "a", "1");
        LayeredConfig config = new LayeredConfig(List.of(props("high", false), low));

        assertThat(config.find("a"))
                .get()
                .extracting(LayeredConfig.Resolved::source)
                .isSameAs(low);
    }

    public void secretFromSecretSafeSourceIsReturnedMasked() {
        LayeredConfig config =
                new LayeredConfig(List.of(new EnvironmentVariableSource(Map.of("API_AUTH_TOKEN", "s3cr3t"))));

        assertThat(config.secret("auth.token")).get().satisfies(secret -> {
            assertThat(secret.reveal()).isEqualTo("s3cr3t");
            assertThat(secret.toString()).doesNotContain("s3cr3t");
        });
    }

    public void secretFromUnsafeSourceIsRejectedEvenIfASafeOneHasIt() {
        LayeredConfig config = new LayeredConfig(List.of(
                props("system property", false, "auth.token", "leaked"),
                new EnvironmentVariableSource(Map.of("API_AUTH_TOKEN", "safe"))));

        assertThatThrownBy(() -> config.secret("auth.token"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("system property")
                .hasMessageContaining("API_AUTH_TOKEN")
                .hasMessageNotContaining("leaked");
    }

    public void environmentVariableNamesAreUpperSnakeCase() {
        assertThat(EnvironmentVariableSource.variableName("baseUrl")).isEqualTo("API_BASE_URL");
        assertThat(EnvironmentVariableSource.variableName("timeout.read.ms")).isEqualTo("API_TIMEOUT_READ_MS");
        assertThat(EnvironmentVariableSource.variableName("auth.oauth2.clientSecret"))
                .isEqualTo("API_AUTH_OAUTH2_CLIENT_SECRET");
    }
}
