# api-test-framework

[![CI](https://github.com/OmarMokhtar-Saad/api-test-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/OmarMokhtar-Saad/api-test-framework/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A reference framework for REST API test automation in Java: **Java 21, TestNG, REST Assured, AssertJ,
Allure**. It is built to be read, copied and extended. It is parallel-safe, keeps secrets out of logs and
reports, and runs fully offline against WireMock, so `./mvnw verify` works on a fresh clone with no setup.

```java
@Test(groups = {"smoke", "regression"}, description = "Creating a user returns 201 and echoes the input")
public void createsUser() {
    CreateUserRequest request = UserFactory.validMember();

    Response response = users().create(request);
    assertThat(response).hasStatus(201).hasHeader("Location");
    User created = assertThat(response).bodyAs(User.class).actual();
    onCleanup("delete user " + created.id(), () -> users().delete(created.id()));

    assertThat(response).jsonPath("email").isEqualTo(request.email());
}
```

## Quick start

Requires Java 21 or newer. Maven comes with the wrapper.

```bash
git clone https://github.com/OmarMokhtar-Saad/api-test-framework.git
cd api-test-framework
./mvnw -B verify                 # every unit + example API test, offline
./mvnw verify -Dsuite=smoke      # one suite: all | unit | smoke | regression | contract
./mvnw allure:serve              # open the report
```

Against a real environment (see [docs/CONFIGURATION.md](docs/CONFIGURATION.md)):

```bash
export API_AUTH_OAUTH2_CLIENT_SECRET=...   # secrets: environment or git-ignored file only
./mvnw verify -Denv=staging -Dsuite=smoke
```

## What you get

| Concern | How it is handled |
|---|---|
| Configuration | Layered: `-D` > `API_*` env vars > `<env>.secrets.properties` > `<env>.properties` > `common.properties`. Validated at start-up with messages that say how to fix the problem. |
| Secrets | Accepted only from env vars or a git-ignored file. A `-D` secret or one in a committed file fails the run. Masked `toString()`. |
| HTTP client | Immutable `ApiClient`. No global REST Assured state, so tests can run with `parallel="methods"`. |
| Auth | `none`, `basic`, `apikey`, `bearer`, `oauth2` (client credentials, with a thread-safe token cache). |
| Assertions | `assertThat(response).hasStatus(201).hasHeader("Location").matchesSchema(...)`, `jsonPath(...)`, `bodyAs(...)`. Failure messages include the redacted HTTP exchange. |
| Reporting | Allure. Each exchange is captured **already redacted** and attached for failed tests (or all, or none). |
| Retries | Only for transport failures (`IOException`). Never for assertion failures. |
| Test data | Seeded Datafaker per thread, with a logged seed for reproducible runs and unique run-scoped emails and IDs. |
| Cleanup | `onCleanup(...)` registers LIFO cleanups that always run and never mask the test result. |
| Quality gates | Enforcer (Java/Maven version, dependency convergence), `-Werror`, Spotless (Palantir format), JaCoCo report, CI on Java 21 and 25. |

## Project layout

```
src/main/java/.../apitest/framework/   domain-agnostic framework (the reusable part)
    config/  auth/  http/  assertion/  logging/  retry/  data/  json/  testng/
src/test/java/.../apitest/
    example/users/     example domain: UsersApi client, records, UserFactory
    tests/users/       example API tests (CRUD, list, validation, contract)
    unit/              framework unit tests (group "unit")
    framework/testng/  unit tests that need package-private listener access
    support/           MockApiServerListener (starts WireMock for env=mock)
src/test/resources/
    env/               common, mock, example template (+ git-ignored *.secrets.properties)
    suites/            TestNG suites: all, unit, smoke, regression, contract
    wiremock/          stub mappings for the offline example API
    schemas/           JSON schemas for contract tests
```

## Documentation

| Read this | When you want to |
|---|---|
| [Getting started](docs/GETTING_STARTED.md) | set up, run, and look at the first report |
| [Writing tests](docs/WRITING_TESTS.md) | add an API, a client, a test, a suite |
| [Configuration](docs/CONFIGURATION.md) | add an environment, pass secrets, look up a key |
| [Architecture](docs/ARCHITECTURE.md) | understand the design, the SOLID mapping and the dependency policy |
| [Coding standards](docs/CODING_STANDARDS.md) | write code that passes review (includes the review checklist) |
| [Development lifecycle](docs/SDLC.md) | branch, commit, open a PR, release |
| [Reporting](docs/REPORTING.md) | read Allure output and HTTP attachments |
| [Troubleshooting](docs/TROUBLESHOOTING.md) | fix a failing build or a confusing error |
| [Decision records](docs/adr/README.md) | see why things are the way they are |
| [AGENTS.md](AGENTS.md) | rules for AI coding agents working in this repo |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security issues: [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE) © Omar Mokhtar
