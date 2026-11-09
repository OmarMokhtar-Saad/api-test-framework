# Security policy

## Reporting a vulnerability

Report it privately through
[GitHub security advisories](https://github.com/OmarMokhtar-Saad/api-test-framework/security/advisories/new).
Do not open a public issue. Expect an acknowledgement within a week.

Examples of what counts:

- a secret or token that reaches a log, a failure message or an Allure attachment unredacted;
- a way to make the framework send credentials to a host other than the configured `baseUrl`;
- a vulnerable dependency that the build does not flag.

## Supported versions

Only the latest commit on `main` is supported.

## How this repository handles secrets

The rules are enforced in code, not only by convention. Details: [docs/CONFIGURATION.md](docs/CONFIGURATION.md#secrets).

- Secrets are read only from environment variables or from a git-ignored `<env>.secrets.properties` file.
- A secret passed as a `-D` system property, or found in a committed properties file, fails the run.
- Secrets are held in a `Secret` type whose `toString()` is masked.
- HTTP exchanges are redacted when they are captured, before they reach logs, failure messages or reports.
- `src/test/resources/env/mock.secrets.properties` is committed on purpose. It holds fake values that only
  the local WireMock server accepts.

If you use this framework against a real system, keep the real credentials in your CI secret store and
expose them as `API_*` environment variables.
