# Architecture decision records

Each record captures one decision: the context, what was decided, and what it costs.
Records are immutable once accepted. To change a decision, add a new record that supersedes the old one
and set the old one's status to `Superseded by NNNN`.

| No. | Decision | Status |
|-----|----------|--------|
| [0001](0001-java-records-over-lombok.md) | Java records and plain code over Lombok | Accepted |
| [0002](0002-hand-written-config-loader.md) | A hand-written layered config loader over the Owner library | Accepted |
| [0003](0003-own-redacting-http-capture.md) | Own redacting HTTP capture over allure-rest-assured | Accepted |
| [0004](0004-no-global-rest-assured-state.md) | No global REST Assured state; an immutable client | Accepted |
| [0005](0005-surefire-3-5-for-testng-suites.md) | Hold Surefire on 3.5.x to keep TestNG suite files | Accepted |

## Template

```markdown
# NNNN. Title

- Status: Proposed | Accepted | Superseded by NNNN
- Date: YYYY-MM-DD

## Context
## Decision
## Consequences
```
