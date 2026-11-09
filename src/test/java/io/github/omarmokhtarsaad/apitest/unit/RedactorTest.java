package io.github.omarmokhtarsaad.apitest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.omarmokhtarsaad.apitest.framework.logging.Redactor;
import java.util.Set;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class RedactorTest {

    private final Redactor redactor = new Redactor();

    public void masksSensitiveHeaderLines() {
        String text = "Accept: application/json\nAuthorization: Basic dXNlcjpwYXNz\nset-cookie: SESSION=abc";

        assertThat(redactor.redact(text))
                .contains("Accept: application/json")
                .contains("Authorization: [REDACTED]")
                .contains("set-cookie: [REDACTED]")
                .doesNotContain("dXNlcjpwYXNz", "SESSION=abc");
    }

    public void masksBearerTokensAnywhere() {
        assertThat(redactor.redact("token was bearer eyJhbGciOi.payload.sig here"))
                .isEqualTo("token was bearer [REDACTED] here");
    }

    public void masksJsonMembersOfAnyScalarType() {
        String json = "{\"name\":\"Ann\",\"password\":\"p\\\"w\",\"PIN\":1234,\"otp\":true,\"email\":\"a@b.test\"}";

        assertThat(redactor.redact(json))
                .contains("\"name\":\"Ann\"")
                .contains("\"password\":\"[REDACTED]\"")
                .contains("\"PIN\":\"[REDACTED]\"")
                .contains("\"otp\":\"[REDACTED]\"")
                .contains("\"email\":\"[REDACTED]\"")
                .doesNotContain("1234", "a@b.test", "p\\\"w");
    }

    public void masksQueryAndFormParameters() {
        String result = redactor.redact("POST /token?client_id=demo&client_secret=abc\ngrant_type=x&password=pw");

        assertThat(result)
                .contains("client_id=demo")
                .contains("client_secret=[REDACTED]")
                .contains("password=[REDACTED]")
                .doesNotContain("=abc", "=pw");
    }

    public void appliesExtraKeysToHeadersAndBodies() {
        Redactor custom = new Redactor(Set.of(" NationalId ", "X-Tenant-Key"));

        String result = custom.redact("X-Tenant-Key: t-1\n{\"nationalId\":\"123\"}\nnationalid=123");

        assertThat(result).doesNotContain("t-1", "123");
    }

    public void isNullSafe() {
        assertThat(redactor.redact(null)).isEmpty();
        assertThat(redactor.redact("")).isEmpty();
    }
}
