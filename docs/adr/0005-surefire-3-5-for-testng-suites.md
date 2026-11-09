# 0005. Hold Surefire on 3.5.x to keep TestNG suite files

- Status: Accepted
- Date: 2026-11-09

## Context

Test selection is done with TestNG suite files under `src/test/resources/suites/` (`all`, `unit`,
`smoke`, `regression`, `contract`), chosen with `-Dsuite=<name>`. The suite files also set
`parallel` and `thread-count`.

Surefire 3.6.0 runs TestNG through the JUnit Platform and removed the `suiteXmlFiles` parameter.
With 3.6.x the build fails with: *Parameter "suiteXmlFiles" is not supported since 3.6.0*.

## Decision

Pin `maven-surefire-plugin` to the latest 3.5.x release (3.5.6) and keep the suite files.

## Consequences

- Suites stay the single, readable place that defines what runs and how parallel it is.
- We do not get Surefire 3.6 features. 3.5.x still receives the fixes we need today.
- Revisit when either TestNG's JUnit Platform engine supports suite files, or the project moves to
  group-based selection (`-Dgroups=smoke`) with parallelism configured in the POM. That move would need
  a new ADR superseding this one, and updates to `docs/WRITING_TESTS.md` and CI.
