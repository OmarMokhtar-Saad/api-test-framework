## What and why

<!-- One or two sentences: the change and the reason for it. Link the issue: Closes #123 -->

## Type

- [ ] Framework (`src/main`)
- [ ] Example domain or tests (`src/test`)
- [ ] Build, CI or docs only

## Checklist

See [docs/CODING_STANDARDS.md#review-checklist](../docs/CODING_STANDARDS.md#review-checklist) for the full list.

- [ ] `./mvnw -B verify` is green locally (format, enforcer, unit + API tests, coverage)
- [ ] New framework behaviour has a unit test in the `unit` group
- [ ] New API tests are in a suite and carry the right groups (`smoke`, `regression`, `contract`)
- [ ] No secrets, tokens, real hostnames or company/product names in code, config, fixtures or logs
- [ ] No global REST Assured state (`RestAssured.*` setters) and no shared mutable state between tests
- [ ] Docs updated (README, `docs/`, `AGENTS.md`) if behaviour, config keys or conventions changed
- [ ] `CHANGELOG.md` updated under **Unreleased**
- [ ] A new design decision has an ADR in `docs/adr/`

## How it was tested

<!-- Commands run, suites, environments. -->
