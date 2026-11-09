# Getting started

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 or newer | CI tests 21 and 25. The enforcer rejects older versions. |
| Maven | none needed | `./mvnw` downloads Maven 3.9.15. A system Maven must be 3.9 or newer. |
| Allure CLI | optional | `./mvnw allure:serve` downloads it for you. |
| IDE | any | IntelliJ IDEA: import as a Maven project. No plugins are required (no Lombok). |

Check: `java -version` must report 21 or newer.

## First run

```bash
./mvnw -B verify
```

This runs, in order: the enforcer, Spotless check, compile with `-Werror`, then the `all` suite (framework
unit tests plus the example API tests against WireMock), then the JaCoCo report. Expect
`Tests run: 66, Failures: 0` (the count grows as tests are added) and `BUILD SUCCESS`.

What happened behind the scenes:

1. No `-Denv` was given, so the environment is `mock`.
2. `MockApiServerListener` started WireMock on a free port, loaded the stubs from
   `src/test/resources/wiremock/`, and pointed `baseUrl` and `auth.oauth2.tokenUrl` at it.
3. The framework loaded `env/common.properties`, `env/mock.properties` and `env/mock.secrets.properties`,
   and logged one line: `Configuration loaded: env=mock baseUrl=http://localhost:... auth=oauth2 ...`.
4. Each API test got an OAuth2 token (cached and shared), made its calls and asserted on them.

## Everyday commands

```bash
./mvnw -q spotless:apply                 # format the code (the build fails on unformatted code)
./mvnw verify -Dsuite=unit               # framework unit tests only (fast)
./mvnw verify -Dsuite=smoke              # a suite: all | unit | smoke | regression | contract
./mvnw verify -Dthreads=8                # override the suite's thread count
./mvnw verify -Dtest=UsersCrudTest      # one class (overrides the suite; listeners still load)
./mvnw verify -Denv=staging              # a real environment (see CONFIGURATION.md)
./mvnw allure:serve                      # build and open the Allure report
```

Reports after a run:

| Path | Content |
|---|---|
| `target/surefire-reports/` | TestNG and JUnit XML, `emailable-report.html` |
| `target/allure-results/` | raw Allure results, including HTTP exchange attachments |
| `target/site/jacoco/index.html` | coverage of `src/main` |

## Running from the IDE

Run a suite file (`src/test/resources/suites/*.xml`) or a single class or method with the TestNG runner.
The listeners register themselves through `META-INF/services`, so nothing else is needed.
To target another environment, add `-Denv=<name>` to the run configuration's VM options. For Allure step
capture in the IDE, also add the AspectJ agent to the VM options; the exact `-javaagent` value is the
`argLine` in `pom.xml`.

## Next

- Write your first test: [WRITING_TESTS.md](WRITING_TESTS.md)
- Point it at a real API: [CONFIGURATION.md](CONFIGURATION.md)
- Understand the pieces: [ARCHITECTURE.md](ARCHITECTURE.md)
