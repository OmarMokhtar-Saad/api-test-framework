# Contributing

Thanks for helping. This file is the short version; the details live in `docs/`.

## Before you start

- Java 21 or newer. Maven is not needed: use the wrapper (`./mvnw`).
- Read [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md), then
  [docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md).
- For anything bigger than a small fix, open an issue first so the approach can be agreed.

## The loop

```bash
git switch -c feat/my-change
# write a failing test, then the code
./mvnw -q spotless:apply
./mvnw -B verify
```

The whole flow, including commit messages, pull requests and releases, is in
[docs/SDLC.md](docs/SDLC.md).

## Ground rules

- `./mvnw -B verify` must be green. It checks formatting, dependency convergence and the Java version,
  and runs every unit and example API test offline.
- Framework code (`src/main`) stays domain-agnostic. Domain clients, models and tests go in `src/test`.
- No secrets, tokens, real hostnames, or company or product names, anywhere: code, config, fixtures,
  commit messages.
- Do not mutate global REST Assured state, and do not share mutable state between tests.
- Update the docs and `CHANGELOG.md` in the same pull request as the behaviour.

## AI agents

Agents follow [AGENTS.md](AGENTS.md). It is the canonical rule set for automated contributors and is
kept in line with these docs.

## License

By contributing you agree that your work is released under the [MIT License](LICENSE).
