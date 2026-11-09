# Coding standards

The build enforces most of these rules, so `./mvnw -B verify` catches them before review. The rest are
checked by the reviewer with the [review checklist](#review-checklist).

## Enforced by the build

| Rule | Tool | Fix |
|---|---|---|
| Palantir Java Format, unused imports removed, import order, no trailing whitespace, final newline | Spotless (`check` bound to `validate`) | `./mvnw -q spotless:apply` |
| No compiler warnings (`-Xlint:all -Werror`) | maven-compiler-plugin | Fix the warning. Do not add `@SuppressWarnings` without a comment saying why. |
| Java 21 or newer, Maven 3.9 or newer | Enforcer | Install the right JDK; always use `./mvnw`. |
| One version per dependency (`dependencyConvergence`, no duplicate versions) | Enforcer | Pin the version in `<dependencyManagement>`. See [ARCHITECTURE.md](ARCHITECTURE.md#dependency-policy). |

Because Spotless and the compiler run in every build, a formatting drift or a single warning fails even
`./mvnw test`.

## Design rules

- **Framework code is domain-agnostic.** `src/main` knows nothing about users, orders or any real API.
  Domain clients, models, factories and tests go in `src/test`.
- **Records for data.** Request and response bodies, config and value types are Java records. No
  Lombok ([ADR 0001](adr/0001-java-records-over-lombok.md)).
- **Immutable shared objects.** `FrameworkConfig`, `ApiClient` and `AuthStrategy` are immutable.
  Methods like `ApiClient.withoutAuth()` return a new instance.
- **No global REST Assured state** ([ADR 0004](adr/0004-no-global-rest-assured-state.md)). Never assign
  `RestAssured.baseURI`, `RestAssured.filters(...)`, `RestAssured.config` or any other static field.
  Requests start from `ApiClient.request()`. `RestAssured.given()` is called only inside
  `framework/http` and `framework/auth`.
- **Configuration only through `config`.** Read settings with `ConfigProvider.get()` (or `config()` in a
  test). No `System.getProperty` or `System.getenv` outside the `framework/config` package. The one
  exception is `support/MockApiServerListener`, which calls `System.setProperty` for `baseUrl` and
  `auth.oauth2.tokenUrl` so the mock server's port wins.
- **No per-test state in fields.** Tests run with `parallel="methods"`. Use locals, `ThreadLocal`
  helpers from the framework, or `onCleanup`.
- **Small interfaces, composition.** New behaviour plugs in through the extension points in
  [ARCHITECTURE.md](ARCHITECTURE.md) (`AuthStrategy`, `TokenProvider`, `ConfigSource`, REST Assured
  filters), not by editing switch statements across the code.
- **Fail with a message that says what to do.** Configuration errors name the key, the environment
  variable and the file to set it in. Follow the existing messages in `ConfigLoader`.
- **Don't swallow exceptions.** Catch only what you handle; rethrow with context otherwise. The one
  deliberate exception is cleanup in `BaseApiTest`, which logs and attaches.

## Naming

| Thing | Convention | Example |
|---|---|---|
| Packages | lower case, under `io.github.omarmokhtarsaad.apitest` | `framework.auth`, `example.users` |
| Client | `<Resource>Api` | `UsersApi` |
| Factory | `<Resource>Factory` | `UserFactory` |
| Request body | `<Verb><Resource>Request` | `CreateUserRequest` |
| Test class | `<Resource><Aspect>Test` | `UsersCrudTest`, `UsersContractTest` |
| Test method | third-person behaviour, no `test` prefix | `createsUser`, `unknownUserReturns404` |
| Config key | dotted, camelCase segments | `auth.oauth2.clientSecret` (env var `API_AUTH_OAUTH2_CLIENT_SECRET`) |
| Constants | `UPPER_SNAKE_CASE` | `MAX_ENTRIES` |

## Tests

- Every `@Test` has a `description` and at least one group (`smoke`, `regression`, `contract` or
  `unit`).
- Arrange, act, assert. One behaviour per test.
- Assert with `ApiAssertions.assertThat`; do not hand-roll status checks.
- Register `onCleanup` right after creating data.
- New framework code comes with unit tests in `src/test/java/.../unit/`. Write the failing test first.
- Tests pass offline: anything that calls HTTP has WireMock stubs.

## Logging and secrets

- Use SLF4J (`LoggerFactory.getLogger(...)`), never `System.out`.
- Never log a secret. Wrap secret values in `Secret`, whose `toString()` is masked. HTTP exchanges are
  redacted at capture time ([ADR 0003](adr/0003-own-redacting-http-capture.md)).
- Secrets come only from environment variables or a git-ignored `*.secrets.properties` file. See
  [CONFIGURATION.md](CONFIGURATION.md#secrets). The committed `mock.secrets.properties` holds a fake
  value for the offline mock and is the only exception.
- No real hostnames, internal IPs, personal data, or company or product names anywhere: code, config,
  fixtures, docs, commit messages. Use `example.com`, `example.test` and generated data.

## Documentation

- Public classes and non-obvious methods get Javadoc explaining *why*, not restating the signature.
- A change to behaviour, a config key or a convention updates the matching doc, `AGENTS.md` if it
  affects agents, and `CHANGELOG.md`, in the same pull request.
- A decision that is hard to reverse gets an ADR in `docs/adr/`.

## Review checklist

The reviewer ticks each item, or the author explains why it does not apply.

**Build and tests**

- [ ] `./mvnw -B verify` is green locally and in CI (Java 21 and 25).
- [ ] New behaviour has tests; a bug fix has a test that failed before the fix.
- [ ] Tests pass offline against `env=mock`, with WireMock stubs for any new endpoint.
- [ ] Every new `@Test` has a `description` and a group.

**Design**

- [ ] `src/main` stays domain-agnostic.
- [ ] No global REST Assured state; requests start from `ApiClient.request()`.
- [ ] No `System.getProperty` / `System.getenv` outside `framework/config`.
- [ ] Shared objects are immutable; tests keep no state in fields.
- [ ] Data is records; no Lombok.
- [ ] Created data is cleaned up with `onCleanup`; tests do not modify data they did not create.

**Security and hygiene**

- [ ] No secrets, tokens, real hostnames, internal IPs, personal data, or company or product names.
- [ ] New sensitive fields or headers are redacted (default list in `Redactor`, or `redact.extraKeys`).
- [ ] No new `@SuppressWarnings` without a reason in a comment.
- [ ] New dependencies are justified, pinned and converge ([dependency policy](ARCHITECTURE.md#dependency-policy)).

**Docs**

- [ ] Docs, `AGENTS.md` and `CHANGELOG.md` updated where behaviour, config or conventions changed.
- [ ] Commit messages follow [SDLC.md](SDLC.md).
