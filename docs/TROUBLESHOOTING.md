# Troubleshooting

Find the message you see, then apply the fix. If it is not here, open an issue with the full output of
`./mvnw -B verify` (redact anything sensitive first).

## Build failures

### `The following files had format violations` (Spotless)

The build checks formatting in the `validate` phase, so it fails before compiling.

```bash
./mvnw -q spotless:apply
```

Then re-run. Configure your IDE with the Palantir Java Format plugin to avoid it.

### `warnings found and -Werror specified`

The compiler runs with `-Xlint:all -Werror`: any warning fails the build, including in tests. Read the
`[WARNING]` line above the error and fix the cause (raw types, unchecked casts, deprecated APIs; the
`serial` lint is off). Add `@SuppressWarnings` only with a comment explaining why, as
`RetryAnnotationTransformer` does.

### `Dependency convergence error` / `Rule 'dependencyConvergence' failed` (Enforcer)

Two dependencies pull in different versions of the same library. The message lists both paths. Pin the
version in `<dependencyManagement>` in `pom.xml`, choosing the newer one unless it breaks something. See
[ARCHITECTURE.md](ARCHITECTURE.md#dependency-policy).

### `Detected JDK version ... is not in the allowed range [21,)` / Maven version rule (Enforcer)

Use JDK 21 or newer, and always run `./mvnw` (it downloads the pinned Maven 3.9.x) instead of a system
`mvn`.

### `Parameter "suiteXmlFiles" is not supported since 3.6.0`

Someone upgraded `maven-surefire-plugin` to 3.6 or later. Keep `surefire-plugin.version` on 3.5.x; the
reason is in [ADR 0005](adr/0005-surefire-3-5-for-testng-suites.md). Dependency update bots must not
bump it past 3.5.

### `There are test failures` but the console shows nothing useful

Look for the `WARN ... HTTP exchange:` lines just above the failure; they show the redacted request and
response. Full details are in `target/surefire-reports/` and in Allure (`./mvnw allure:serve`). See
[REPORTING.md](REPORTING.md).

## Configuration errors

Configuration is read once, on first use, and validated as a whole. Errors are
`ConfigurationException`s that name the key and where to set it. Full key list:
[CONFIGURATION.md](CONFIGURATION.md).

### Many tests fail with `NoClassDefFoundError: Could not initialize class ...ConfigProvider$Holder`

The configuration failed to load in an earlier test, and the JVM will not retry it. Scroll up to the
**first** failure: it is an `ExceptionInInitializerError` whose cause is the real
`ConfigurationException`. Fix that one.

### `Required configuration '<key>' is not set for environment '<env>'`

Set the key as `-D<key>=...`, as the environment variable named in the message (`API_...`), or in
`src/test/resources/env/<env>.properties`.

### `Required secret '<key>' is not set for environment '<env>'`

Secrets are read only from the environment variable named in the message or from the git-ignored file
`src/test/resources/env/<env>.secrets.properties`. See [CONFIGURATION.md](CONFIGURATION.md#secrets).

### `Secret '<key>' was supplied via system property, which is not allowed because it leaks the value`

A secret passed as `-D...` would appear in process lists and CI logs, so it is rejected, and the
framework does not fall back to the secrets file. Remove the `-D` and export the environment variable
instead:

```bash
export API_AUTH_OAUTH2_CLIENT_SECRET=...
./mvnw verify -Denv=staging
```

The same error with `file env/<env>.properties` or `file env/common.properties` means the secret was put
in a committed file. Move it to `<env>.secrets.properties` or an environment variable, and rotate it if it
was ever pushed.

### A secret I set is ignored

Secrets are only checked for the configured `auth.type`. With `auth.type=none`, an `auth.token` is never
read. Check the `auth=` value in the `Configuration loaded:` line.

### `Environment name '<x>' is invalid`

Environment names are lower-case letters, digits and `-`, up to 31 characters: `qa`, `staging`,
`perf-2`.

### `'baseUrl' must be an absolute http(s) URL ...` / `must not embed credentials`

Use a full URL like `https://api.example.com`. Credentials go in the `auth.*` keys, never in the URL.

### `'<key>' must be greater than zero` / `must be an integer` / `must be zero or more` / `must be one of ...`

The message quotes the value it got. Fix the value; check for stray spaces or units (`5000`, not `5s`).

### `'threads' must be >= 1` / `'threads' must be an integer`

`-Dthreads` / `API_THREADS` must be a positive whole number. Leave it unset to use the suite's
`thread-count`.

## Surprising behaviour

### `-DbaseUrl=...` is ignored

With `env=mock` (the default), `MockApiServerListener` starts WireMock and overwrites `baseUrl` and
`auth.oauth2.tokenUrl` with the mock's address. To hit a real API, select its environment:
`-Denv=staging`.

### A `-D` setting changed in a test or listener has no effect

`ConfigProvider` loads once per JVM, on the first `get()`. Later changes to system properties are not
seen. Set everything on the command line or in the environment before the run.

### Retries do not happen / cannot be disabled for one test

Only failures caused by an `IOException` (timeouts, connection resets) are retried; assertion failures
and HTTP error statuses never are. There is no per-test opt-out:
`@Test(retryAnalyzer = DisabledRetryAnalyzer.class)` is overridden by the framework. Use
`-Dretry.max=0` to disable retries for the run.

### A cleanup never ran

Cleanups are thread-local. `onCleanup` must be called on the test method's own thread, not inside an
executor, `CompletableFuture` or Awaitility callback. Cleanups that throw are logged and attached to
Allure as `Cleanup failed: ...`.

### Emails show as `[REDACTED]` in reports

Intended: `email` and `phone` fields are redacted as personal data. See
[REPORTING.md](REPORTING.md#redaction).

### My new test class does not run

- API tests must be under the `...apitest.tests` package (suites select `...apitest.tests.*`) and carry
  a group the suite includes.
- Framework unit tests must be under `...apitest.unit` with `groups = "unit"`, or be listed by class
  name in `suites/unit.xml` and `suites/all.xml` (as `ListenerRulesTest` is).
- `-Dtest=MyTest` runs a single class regardless of suites.

### Tests interfere with each other when run in parallel

Tests run with `parallel="methods"`. Look for state in fields, shared fixed records (such as user `1`
in the examples), or reused emails. Use `TestDataFactory.uniqueEmail()` and create your own data; see
[WRITING_TESTS.md](WRITING_TESTS.md#create-your-own-data). To confirm, run with `-Dthreads=1`.
