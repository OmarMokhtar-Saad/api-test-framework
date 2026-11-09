# 0003. Own redacting HTTP capture over allure-rest-assured

- Status: Accepted
- Date: 2026-11-09

## Context

`allure-rest-assured` attaches every request and response to the report. It does so verbatim:
`Authorization` headers, tokens in JSON bodies and passwords in form posts all land in the report,
which is often published as a CI artifact. The earlier frameworks also logged every exchange with
REST Assured's `log().all()`, which sends the same secrets to the console.

## Decision

The framework captures exchanges itself. `HttpExchangeRecorder` is a REST Assured filter that records
each exchange into a per-thread `ExchangeLog`, passing headers, query and form parameters and JSON bodies
through `Redactor` **at capture time**. Nothing unredacted is ever stored.
`HttpReportingListener` attaches the captured exchanges to Allure according to the `HttpLogPolicy`
(for example only for failed tests), and `ResponseAssert` failure messages quote the redacted exchange.

## Consequences

- Reports and logs are safe to publish by default; there is no unredacted copy to leak.
- Redaction rules live in one class with their own unit tests (`RedactorTest`).
- The attachment format is ours to keep readable.
- We maintain a small amount of capture code instead of using a ready-made integration.
- Redaction is key-based. A secret placed under an innocent key is not detected; extra keys can be added
  through configuration.
