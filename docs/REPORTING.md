# Reporting

Every run writes three kinds of output under `target/`:

| Path | Content | Open with |
|---|---|---|
| `target/allure-results/` | raw Allure results: steps, HTTP exchange attachments, cleanup failures | `./mvnw allure:serve` |
| `target/surefire-reports/` | TestNG and JUnit XML, `emailable-report.html`, per-class `.txt` summaries | a browser, or CI test summaries |
| `target/site/jacoco/` | JaCoCo coverage of the framework code (report only, no threshold) | `index.html` |

CI uploads all three as build artifacts.

## Allure

```bash
./mvnw verify            # produce target/allure-results
./mvnw allure:serve      # build the report into a temp dir and open it in a browser
./mvnw allure:report     # build a static report under target/site/ (for publishing)
```

Each test shows:

- its `description` and groups;
- one step per client call, named by the `@Step` on the client method (for example
  `POST /users`);
- the HTTP exchanges it made, as attachments (see below);
- a `Cleanup failed: <description>` attachment with the stack trace if a cleanup threw.

## HTTP exchange attachments

`HttpExchangeRecorder` is a REST Assured filter on every request from `ApiClient`. It records the
request and the response (or the transport error, such as a timeout) into a per-test, thread-local
`ExchangeLog`. `HttpReportingListener` decides after each test whether to publish them.

`http.log` (env var `API_HTTP_LOG`) sets the policy:

| Value | Publishes exchanges for |
|---|---|
| `FAILURES` (default) | every test that did not pass: failed **and skipped** tests |
| `ALL` | every test |
| `NONE` | no test; nothing is recorded at all |

For each published exchange:

- an Allure attachment of type `text/plain` with a `.txt` extension, titled
  `<METHOD> <url> -> <status> (<ms> ms)`, or `-> no response` for transport errors;
- for a test that did not pass, the same text is logged at `WARN` on the console, so CI logs show it
  without opening Allure.

The attachment body looks like:

```
POST http://localhost:51234/api/v1/users -> 201 (12 ms)

--- Request ---
...headers and body...

--- Response ---
...headers and body...
```

Limits:

- At most 100 exchanges are kept per test; beyond that the oldest are dropped, so the last 100 remain.
  A test that makes more calls than that is probably doing too much.
- Request and response bodies are cut at `http.log.maxBodyChars` (default 20000).

## Redaction

Values are redacted **when they are recorded**, before they reach memory, the console or Allure
([ADR 0003](adr/0003-own-redacting-http-capture.md)). Redacted values appear as `[REDACTED]`.

Redacted by default (case-insensitive):

- headers: `Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`, `X-API-Key`, `Api-Key`,
  `X-Auth-Token`, `X-CSRF-Token`, and the configured `auth.apikey.header`;
- any `Bearer <token>` text;
- JSON members, query parameters and form fields named `password`, `pass`, `pwd`, `token`,
  `accessToken`, `access_token`, `refreshToken`, `refresh_token`, `idToken`, `id_token`, `otp`, `pin`,
  `secret`, `clientSecret`, `client_secret`, `apiKey`, `api_key`, `authorization`, `phone`,
  `phoneNumber`, `phone_number`, `mobile`, `mobileNumber`, `email`, `emailAddress`.

`email` and `phone` are on the list because they are personal data. That is why the example user
emails show as `[REDACTED]` in attachments. To redact more fields, set `redact.extraKeys`
(`API_REDACT_EXTRA_KEYS`), a comma-separated list:

```bash
./mvnw verify -Dredact.extraKeys=nationalId,dateOfBirth
```

There is no setting that turns redaction off.

## Failure messages

A failed `ResponseAssert` check ends its message with the context of the call:

- the last recorded exchange for the test, rendered as above; or
- if nothing was recorded (`http.log=NONE`), the response body, redacted with the default list and
  truncated at 2000 characters.

Note that this fallback uses only the default redaction list, not `redact.extraKeys`. Keep
`http.log` at `FAILURES` or `ALL` on environments with extra sensitive fields.

## Reproducing a run

The first configuration access logs one line:

```
Configuration loaded: env=mock baseUrl=http://localhost:51234 basePath='/api/v1' auth=oauth2 httpLog=FAILURES retryMax=1 dataSeed=42
```

`dataSeed` seeds the Faker instance in `TestDataFactory`. Re-run with the same seed to get the same
generated names and values:

```bash
./mvnw verify -Ddata.seed=42 -Dtest=UsersCrudTest
```

Each worker thread has its own Faker built from that seed, so values match exactly when the same tests
run on the same threads in the same order. For a strict reproduction, add `-Dthreads=1`. Unique values
(`uniqueEmail()`, `uniqueSuffix()`) include a per-run id on purpose and always differ between runs.

## Retries in reports

A test retried by `TransientFailureRetryAnalyzer` (only for `IOException` causes) shows its earlier
attempts as skipped in TestNG, and the analyzer logs a `WARN` line per retry. Allure shows the earlier attempts
in the test's Retries tab. A test that passes only after a retry is a flaky signal: look at the
attached exchanges of the skipped attempt.
