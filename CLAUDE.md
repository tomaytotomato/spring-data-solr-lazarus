# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Project Overview

Spring Data Solr Lazarus — a modern Spring Boot starter for Apache Solr 10, resurrecting the
archived `spring-data-solr` project. Personal project (not giffgaff). Targets JDK 25 and Spring Boot
4.0.6.

## Build Commands

```bash
mvn clean verify                    # full build + tests (always clean after BOM bumps)
mvn test                            # unit tests only
mvn test -pl solr-spring-boot-autoconfigure  # tests in autoconfigure module only
mvn test -pl solr-spring-boot-autoconfigure -Dtest=SolrQueryCreatorTest  # single test class
mvn test -pl solr-spring-boot-autoconfigure -Dtest="SolrQueryCreatorTest#createsIsQueryForSimpleProperty"  # single test method
```

Integration tests (`SolrIntegrationTest`) require Docker running — they use Testcontainers with a
Solr 9 container and skip gracefully when Docker is unavailable.

## Module Structure

Three-module Maven project — the classic Spring Boot starter pattern:

- **`solr-spring-boot-autoconfigure`** — all the real code: auto-configuration, template, query
  builder, repository abstraction, health indicator, and all tests
- **`solr-spring-boot-starter`** — thin POM that pulls in autoconfigure + solr-solrj (consumers
  depend on this)
- **`solr-spring-boot-sample`** — demo Spring Boot app with Docker Compose support for local Solr

## Architecture

### Auto-Configuration Chain

Three `@AutoConfiguration` classes registered in
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

1. **`SolrAutoConfiguration`** — creates `SolrClient` (standalone `HttpJdkSolrClient` by default, or
   `CloudSolrClient` when `spring.solr.zk-host` is set) and `SolrTemplate`. Properties bound under
   `spring.solr.*` via `SolrProperties`.
2. **`SolrHealthAutoConfiguration`** (after #1) — registers `SolrHealthIndicator` when Actuator is
   on the classpath. Pings the configured collection, or falls back to admin system info endpoint.
3. **`SolrRepositoryAutoConfiguration`** (after #1) — enables Spring Data repository scanning via
   `@EnableSolrRepositories` / `SolrRepositoriesRegistrar`.

### Template Layer

`SolrOperations` (interface) → `SolrTemplate` (implementation). Wraps SolrJ with collection-aware
CRUD, query execution, partial updates, and commit mode control (`NONE` or `IMMEDIATE`). Uses
`SolrDocumentResolver` to derive collection names from `@SolrDocument` annotations.

### Query System

Two levels:

- **`Criteria`** — fluent builder for individual Solr field predicates, chained with `.and(field)` /
  `.or(field)`. Renders to a Solr query string.
- **`SimpleQuery`** — wraps Criteria with pagination, sort, filter queries, projections, defType.
  Converts to SolrJ `SolrQuery` via `toSolrQuery()`.

### Spring Data Repository

`SolrRepository<T, ID>` extends `PagingAndSortingRepository` + `CrudRepository`. Implemented by
`SimpleSolrRepository` which delegates to `SolrTemplate`.

Derived query methods work through the Spring Data PartTree mechanism:

```
Method name → PartTree → SolrQueryCreator → SimpleQuery → SolrQuery → SolrTemplate
```

`SolrQueryCreator` translates 18 Part.Type keywords (Is, Containing, Between, GreaterThan, In,
IsNull, True, etc.) into Criteria predicates. `PartTreeSolrQuery` dispatches on return type (List,
Page, single, count, exists).

### Document Mapping

Entity classes use `@SolrDocument(collection = "name")` for collection resolution and SolrJ's
`@Field` for field binding. `SolrDocumentResolver` derives collection name from the annotation (
falls back to lowercase class name).

**Known limitation:** derived queries use Java property names as Solr field names. A property named
`year` with `@Field("publication_year")` will query `year:` not `publication_year:`. Proper `@Field`
name mapping in derived queries is not yet implemented.

## Key Dependencies

| Dependency     | Version | Notes                                                                                                            |
|----------------|---------|------------------------------------------------------------------------------------------------------------------|
| Spring Boot    | 4.0.6   | Boot 4 moved health classes from `boot.actuate` to `boot.health`                                                 |
| SolrJ          | 10.0.0  | Split into `solr-solrj` (core), `solr-solrj-jetty`, `solr-solrj-zookeeper`                                       |
| Testcontainers | 2.0.5   | BOM managed explicitly (not by Boot). Artifacts renamed to `testcontainers-solr`, `testcontainers-junit-jupiter` |
| JDK            | 25      | Pinned via `.java-version` (jenv)                                                                                |

## SolrJ 10 Gotchas

- `HttpJdkSolrClient` is the zero-dependency default client (not `Http2SolrClient`, which is now
  `HttpJettySolrClient` in the jetty module)
- Timeout API uses `(long, TimeUnit)` not `(int)`
- `NamedList.findRecursive` doesn't exist — use `instanceof NamedList<?>` pattern matching
- `GenericSolrRequest` uses `SolrRequest.SolrRequestType.ADMIN`
- `CloudSolrClient` is in core `solr-solrj` (not only in the zookeeper module)
- Use `ClientUtils.escapeQueryChars` instead of rolling your own

## Running the Sample App

The sample module uses Spring Boot Docker Compose support. With Docker running:

```bash
mvn spring-boot:run -pl solr-spring-boot-sample
```

This auto-starts a Solr 10 container with a pre-created `books` collection. The app exposes book
CRUD endpoints and Actuator health at `/actuator/health`.

## Claude Code Configuration

The `.claude/` directory contains project-level configuration:

- **`settings.local.json`** — project permissions for Maven, git workflow, GitHub CLI, Docker, and
  jenv. Not committed (local to each developer). Global settings inherit read-only permissions
  (find, grep, ls, git status/log/diff/branch) automatically.
- **`skills/devlog/SKILL.md`** — skill that maintains `DEVLOG.md`. Triggers at the end of work
  sessions or when asked. Gathers git context, test counts, and writes entries in the established
  format. Use it — don't update the devlog manually.

Global hooks (git staging guard, Maven delegation, destructive git gate, pre-push build gate, PR
freshness, gh auth guard) apply automatically from `~/.claude/settings.json`.

## Dev Log

`DEVLOG.md` tracks dated development sessions with architecture decisions, gotchas discovered, and
test counts. Update it after significant sessions — use the `devlog` skill.
