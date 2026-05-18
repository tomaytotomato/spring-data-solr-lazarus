# Contributing

Contributions are welcome. Here is what you need to know.

## Prerequisites

- JDK 21+
- Docker (for integration tests)
- Maven Wrapper included — no local Maven install needed

## Building and testing

```bash
./mvnw clean verify
```

Integration tests run against real Solr containers via Testcontainers and skip gracefully if Docker is unavailable.

## Making changes

- Write tests first. Every change should be driven by a failing test.
- Keep commits focused — one logical change per commit.
- Branch names: `feat/`, `fix/`, `chore/` prefixes.

## Raising a PR

- Fill in the PR template.
- Make sure `./mvnw clean verify` passes locally before pushing.
- If you are changing configuration properties or public API, update the relevant docs too.
