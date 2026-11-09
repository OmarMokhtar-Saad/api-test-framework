# 0001. Java records and plain code over Lombok

- Status: Accepted
- Date: 2026-11-09

## Context

Earlier frameworks this one replaces used Lombok for models and builders. That brought an annotation
processor into every build, IDE plugin requirements, generated code that is invisible during review,
and `@Data` classes that were mutable and had surprising `equals` semantics.
Java 21 offers records, which give immutable value types with `equals`, `hashCode` and `toString` for free.

## Decision

No Lombok. Request and response models are Java records. Where a builder helps (test data with many
optional fields), it is written by hand next to the record, or the factory method takes the few
parameters that vary.

## Consequences

- Plain Java: what you read is what runs, and reviewers see all the code.
- No annotation processor or IDE plugin is needed to build or edit the project.
- Models are immutable by default, which makes parallel tests safer.
- A hand-written builder costs a few more lines than `@Builder`. That is accepted.
