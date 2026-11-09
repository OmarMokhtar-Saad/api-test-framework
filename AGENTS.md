# AGENTS.md

Canonical rules for AI coding agents (and a quick reference for humans) working in this repository.
Tool-specific files such as `CLAUDE.md` only point here. When this file and another doc disagree, fix
the other doc or this one in the same change; do not guess.

## What this repo is

A REST API test framework: Java 21, TestNG, REST Assured, AssertJ, Allure. `src/main` is the reusable,
domain-agnostic framework. `src/test` holds an example domain (`users`) that runs offline against
WireMock, plus the framework's unit tests.

## Commands

```bash
./mvnw -q spotless:apply          # format; run before every verify
./mvnw -B verify                  # the gate: format check, enforcer, -Werror compile, all tests (offline)
./mvnw verify -Dsuite=unit        # suites: all (default) | unit | smoke | regression | contract
./mvnw verify -Dtest=UsersCrudTest
./mvnw verify -Dthreads=1         # rule out parallelism
./mvnw allure:serve               # open the Allure report after a run
```

Always use `./mvnw`, never a system `mvn`. No network access is needed beyond Maven downloads.

## Layout

```
src/main/java/io/github/omarmokhtarsaad/apitest/framework/
    config/ auth/ http/ assertion/ logging/ retry/ data/ json/ testng/
src/main/resources/META-INF/services/org.testng.ITestNGListener   # framework listeners
src/test/java/io/github/omarmokhtarsaad/apitest/
    example/users/     UsersApi client, records, UserFactory
    tests/users/       example API tests
    unit/              framework unit tests (group "unit")
    framework/testng/  ListenerRulesTest (package-private access; listed by class name in suites)
    support/           MockApiServerListener (WireMock for env=mock)
src/test/resources/
    env/  suites/  wiremock/mappings/  schemas/  META-INF/services/
docs/                  handbook and ADRs
```

## Hard rules

1. `./mvnw -B verify` must pass. Do not weaken it: no skipping tests, no disabling Spotless, Enforcer,
   `-Werror` or listeners, no `@Test(enabled = false)` to get green.
2. Never commit secrets, tokens, real hostnames, internal IPs, personal data, or company or product
   names, in code, config, fixtures, docs or commit messages. Use `example.com`, `example.test` and
   generated data. `env/mock.secrets.properties` holds the only committed (fake) secret.
3. Secrets come only from environment variables or git-ignored `*.secrets.properties` files. Never
   pass a secret with `-D`; the framework rejects it.
4. `src/main` stays domain-agnostic. Domain clients, models, factories and tests go in `src/test`.
5. No global REST Assured state: never set `RestAssured.*` static fields. Requests start from
   `ApiClient.request()`.
6. No `System.getProperty` / `System.getenv` outside `framework/config`. (`MockApiServerListener`'s
   `System.setProperty` is the documented exception.)
7. Records for data, no Lombok. Shared objects immutable. No per-test state in fields
   (`parallel="methods"`).
8. Every created record gets `onCleanup(...)`, registered on the test's own thread. Do not modify or
   delete data the test did not create.
9. Do not upgrade `maven-surefire-plugin` past 3.5.x ([ADR 0005](docs/adr/0005-surefire-3-5-for-testng-suites.md)).
10. New dependencies need a reason, a pinned version and convergence
    ([dependency policy](docs/ARCHITECTURE.md#dependency-policy)).
11. Do not push to `main` or publish anything without the maintainer's explicit approval.

## Gotchas

- Config loads once per JVM; changing system properties at runtime has no effect.
- With `env=mock`, `baseUrl` and `auth.oauth2.tokenUrl` are overwritten by the mock server.
- Retry opt-out per test is impossible (`DisabledRetryAnalyzer` is overridden); use `-Dretry.max=0`.
- `email` and `phone` are redacted in reports by design.
- A test outside `tests.*` or `unit` packages is not run by any suite unless listed by class name.

## How to make a change

1. Read the relevant doc below and the code you will touch.
2. Write a failing test (unit test in `unit/` for framework code; API test plus WireMock stub for
   example code).
3. Make it pass with the smallest change that fits the design rules.
4. `./mvnw -q spotless:apply && ./mvnw -B verify`.
5. Update docs, this file if a rule or command changed, and `CHANGELOG.md` under `Unreleased`.
6. Commit with a Conventional Commit message ([SDLC.md](docs/SDLC.md)).

## Definition of done

- [ ] `./mvnw -B verify` green, run after the last edit.
- [ ] New behaviour covered by tests that failed before the change.
- [ ] Every item in the [review checklist](docs/CODING_STANDARDS.md#review-checklist) holds.
- [ ] Docs, `AGENTS.md` and `CHANGELOG.md` updated where behaviour, config or conventions changed.
- [ ] No secrets or company names (`git diff` reviewed before commit).

## Docs

| Need | Doc |
|---|---|
| First run | [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md) |
| Add an API, client, test, suite, stub | [docs/WRITING_TESTS.md](docs/WRITING_TESTS.md) |
| Config keys, environments, secrets | [docs/CONFIGURATION.md](docs/CONFIGURATION.md) |
| Design, extension points, thread safety | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Style rules and review checklist | [docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md) |
| Allure, HTTP attachments, redaction | [docs/REPORTING.md](docs/REPORTING.md) |
| Error messages and fixes | [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) |
| Branching, commits, PRs, releases | [docs/SDLC.md](docs/SDLC.md) |
| Decisions | [docs/adr/](docs/adr/README.md) |
