# 0002. A hand-written layered config loader over the Owner library

- Status: Accepted
- Date: 2026-11-09

## Context

Earlier frameworks used the Owner library for configuration. Owner is no longer actively maintained,
resolves values through interface proxies that are hard to debug, cannot say *where* a value came from,
and has no notion of a secret: a token could come from a `-D` flag, sit in a committed file, and be
printed by `toString()`.

What the framework needs is small: a fixed set of keys, a clear precedence order, validation with
helpful messages, and strict rules for secrets.

## Decision

Configuration is loaded by a small hand-written stack in `framework.config`:
`ConfigSource` implementations (system properties, environment variables, properties files) layered by
`LayeredConfig`, parsed and validated by `ConfigLoader` into an immutable `FrameworkConfig` record, and
exposed through `ConfigProvider`. Every key is declared once in `ConfigKeys`. Secrets are a `Secret` type.

## Consequences

- Each value can report its source (provenance), which makes "why is this the wrong URL?" a one-line answer.
- Secret rules (environment or git-ignored file only, masked `toString()`) are enforced in one place.
- Validation errors name the key, the bad value (unless it is a secret) and the allowed values.
- About 700 lines of code (including Javadoc) to own and test. The unit tests in `LayeredConfigTest` and `ConfigLoaderTest` cover it.
- No dependency to upgrade or to lose to abandonment.
