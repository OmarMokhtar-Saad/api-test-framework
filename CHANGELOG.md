# Changelog

All notable changes to this project are recorded here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `hasJsonPath`, `hasJsonPathSize`, `hasJsonPathContaining`, `hasBody(Class, Consumer)` and
  `ApiAssertions.assertSoftly`; `ChainedAssertionsGuardTest` enforces chained assertions.

- Framework core: layered configuration with provenance and secret handling, immutable `ApiClient`,
  pluggable authentication (none, basic, API key, bearer, OAuth2 client credentials with a caching token
  provider), fluent response assertions with JSON-schema checks, redacting HTTP exchange capture,
  transient-failure retry, and TestNG listeners for reporting and parallelism.
- Example Users domain and API tests (CRUD, list, validation, contract) running offline against WireMock.
- Framework unit tests (`unit` group) and TestNG suites: `all`, `unit`, `smoke`, `regression`, `contract`.
- CI on Java 21 and 25 with test, Allure and coverage artifacts.
- Handbook under `docs/`, agent rules in `AGENTS.md`, and architecture decision records.
