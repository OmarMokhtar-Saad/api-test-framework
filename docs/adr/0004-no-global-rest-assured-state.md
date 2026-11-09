# 0004. No global REST Assured state; an immutable client

- Status: Accepted
- Date: 2026-11-09

## Context

REST Assured exposes static setters (`RestAssured.baseURI`, `RestAssured.filters(...)`,
`RestAssured.config`, `RestAssured.requestSpecification`). Earlier frameworks set these in `@BeforeSuite`
hooks. With parallel tests that caused order-dependent failures, filters registered twice, and auth
headers leaking from one test to another.

## Decision

The framework never writes REST Assured's static state. `ApiClient` is an immutable object built once
from `FrameworkConfig` (base URI, base path, timeouts, auth strategy, filters). Each call to
`request()` returns a fresh `RequestSpecification` derived from it. Variations such as "no auth" or
"a different token" produce a new client rather than mutating the shared one.

## Consequences

- Tests are safe to run with `parallel="methods"`; nothing is shared but immutable values.
- A test can see exactly which client it uses; there is no hidden global setup to trace.
- Unit tests can build an `ApiClient` by hand against their own WireMock server without touching the
  run's configuration.
- Code that copies examples using `RestAssured.given()` directly bypasses auth, timeouts and redaction.
  The coding standards forbid it, and reviewers check for it.
