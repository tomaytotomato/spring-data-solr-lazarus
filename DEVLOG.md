# Spring Data Solr Lazarus — Dev Log

Resurrecting first-class Spring Boot support for Apache Solr. The original `spring-data-solr` is in the Spring attic; Solr 10 deserves a modern starter.

## 2026-05-12 — Day 1: From Zero to Feature-Complete

### Session 1: Scaffolding & Core (20:00–20:30)

**Commits:** `ddc66c7` → `45885b0`

Set up the multi-module Maven project: parent POM with Spring Boot 3.5.0 BOM, SolrJ 10.0.0, Java 21.

- `solr-spring-boot-autoconfigure` — the real work
- `solr-spring-boot-starter` — thin starter POM
- `solr-spring-boot-sample` — demo app

Core auto-configuration: `SolrProperties` binding `spring.solr.*`, `SolrAutoConfiguration` creating `HttpJdkSolrClient` (standalone) or `CloudSolrClient` (when `zkHost` is set). `SolrTemplate` wrapping SolrJ with save/find/query/delete. Sample app with `BookController`. Docker Compose support with Solr 10 container.

**Gotcha:** SolrJ 10 split into three artifacts (`solr-solrj`, `solr-solrj-jetty`, `solr-solrj-zookeeper`). `Http2SolrClient` renamed to `HttpJettySolrClient`. `HttpJdkSolrClient` is the zero-dependency default. Timeout API is `(long, TimeUnit)` not `(int)`.

### Session 2: Critique-Driven Enrichment (22:00–23:00)

**Commits:** `adb926a` → `c4bd35f`

Ran a critique agent against the initial implementation. It flagged the library as "thin" — just a wrapper around SolrJ with no real Spring Data value-add. Fair point. Went deep and built:

- **Criteria API** — fluent query builder (`Criteria.where("title").contains("spring").and("price").greaterThan(10)`)
- **SimpleQuery** — wraps Criteria with pagination, sort, filters, projections → converts to SolrQuery
- **SolrPage** — extends Spring Data `PageImpl` with Solr's `maxScore`
- **@SolrDocument / @Score annotations** — `SolrDocumentResolver` for collection name resolution
- **PartialUpdate** — atomic field updates (set/add/increment) via SolrInputDocument
- **Spring Data Repository abstraction** — `SolrRepository<T,ID>` extending `PagingAndSortingRepository` + `CrudRepository`, with `SimpleSolrRepository`, `SolrRepositoryFactory`, `@EnableSolrRepositories`, and auto-configuration for zero-config scanning
- **SolrHealthIndicator** — collection-aware ping with admin endpoint fallback
- **Testcontainers integration tests** — 4 tests with graceful skip when Docker unavailable

**Gotchas discovered:**
- `NamedList.findRecursive` doesn't exist in Solr 10 — used pattern matching `instanceof NamedList<?>` instead
- `GenericSolrRequest` uses `SolrRequest.SolrRequestType.ADMIN` not its own inner type
- `CloudSolrClient` IS in core `solr-solrj` (not just the zookeeper module) — no reflection needed
- Health indicator needs `@ConditionalOnEnabledHealthIndicator("solr")` to respect `management.health.solr.enabled`
- Rolled our own `escapeQueryChars` initially — replaced with `ClientUtils.escapeQueryChars`

**End of day:** 120 tests (116 unit + 4 integration), BUILD SUCCESS.

---

## 2026-05-13 — Day 2: Derived Query Methods

### Session 1: PartTree → Solr Queries (08:30–09:10)

**Commit:** `74b8bac`

Implemented Spring Data derived query method support — the feature that makes repository interfaces feel magical. Declare `findByTitleContaining(String)` and the Solr query writes itself.

**Architecture:**

```
Repository method name
        ↓
   PartTree (Spring Data Commons)
        ↓
   SolrQueryCreator (AbstractQueryCreator<SimpleQuery, Criteria>)
        ↓
   SimpleQuery → SolrQuery
        ↓
   SolrTemplate.query() / queryForPage() / count()
```

**New classes:**
- `SolrQueryCreator` — translates PartTree keywords to Criteria predicates. 18 Part.Types supported.
- `PartTreeSolrQuery` — implements `RepositoryQuery`, handles return type dispatch (List, Page, single, count, exists)
- `SolrQueryLookupStrategy` — plugs into `SolrRepositoryFactory.getQueryLookupStrategy()`

**Criteria enhancements:**
- `and(Criteria)` / `or(Criteria)` — combine independently-built Criteria chains (needed for OR branches in PartTree)
- `notContains(String)` — negated wildcard matching for `NOT_CONTAINING`
- `SimpleQuery.setPageable()` / `getCriteria()` — accessors for query reuse

**Supported keywords:** Is, Not, Containing, NotContaining, StartingWith, EndingWith, GreaterThan, GreaterThanEqual, LessThan, LessThanEqual, Between, In, IsNull, IsNotNull, True, False, Before, After

**Return types:** `List<T>`, `Page<T>`, single entity, `long` (count), `boolean` (exists)

**Sample app updated:** `BookRepository` now declares derived methods:
```java
List<Book> findByAuthor(String author);
List<Book> findByTitleContaining(String title);
Page<Book> findByAuthorAndYearGreaterThan(String author, int year, Pageable pageable);
long countByAuthor(String author);
boolean existsByTitle(String title);
```

**Tests:** 31 new (21 SolrQueryCreator + 7 PartTreeSolrQuery + 3 Criteria chain combinators). **151 total, 0 failures.**

**Known limitation:** Derived queries use Java property names as Solr field names. If a field has `@Field("publication_year")` but the property is `year`, the derived query queries `year:` not `publication_year:`. Proper @Field mapping is a future enhancement.

### Session 2: Migrate to Spring Boot 4.0.6 + JDK 25 (09:30–10:00)

**Commit:** `1993868`

Bumped from Spring Boot 3.5.0 → 4.0.6, Spring Data Commons 3.5.0 → 4.0.5, targeting JDK 25 (LTS).

**Breaking changes resolved:**

| What moved | Old (Boot 3.5) | New (Boot 4.0) |
|------------|----------------|----------------|
| Health classes | `boot.actuate.health` | `boot.health.contributor` |
| Health auto-config | `boot.actuate.autoconfigure.health` | `boot.health.autoconfigure.contributor` |
| Actuator dependency | `spring-boot-actuator-autoconfigure` | `spring-boot-health` |
| Query lookup strategy | `QueryMethodEvaluationContextProvider` | `ValueExpressionDelegate` |
| Testcontainers artifacts | `solr`, `junit-jupiter` | `testcontainers-solr`, `testcontainers-junit-jupiter` |
| Testcontainers BOM | managed by Boot | explicit `testcontainers-bom` 2.0.5 required |

**Gotcha:** `PropertyPath` moved from `org.springframework.data.mapping` to `org.springframework.data.core` in Spring Data 4.0. Didn't require a code change (we only call `part.getProperty().getSegment()`) but caused a stale-classpath `NoSuchMethodError` until a clean build was run. Lesson: always `mvn clean` after a major BOM bump.

**Also fixed:** `SolrIntegrationTest.TestBook` was package-private — SolrJ's `DocumentObjectBinder` couldn't instantiate it. Made it `public`. This was a latent bug hidden because Docker wasn't running in earlier sessions.

**Tests:** 158 total (147 unit + 11 integration), 0 failures. Full BUILD SUCCESS on JDK 25.0.3 (Temurin LTS).

**Build verified:** `mvn clean verify` — all modules compile and pass on JDK 25, Spring Boot 4.0.6. `.java-version` pinned via jenv.

### Session 3: Claude Code Setup & Documentation (13:00–14:00)

**Commits:** `839bf53` → `992fdc2`

Shifted focus from code to developer experience. Pinned JDK 25 via jenv (`.java-version`), added a
README with project motivation and reference links, and created `CLAUDE.md` — a comprehensive
guidance file for Claude Code covering build commands, module structure, architecture, SolrJ 10
gotchas, and the known `@Field` mapping limitation.

Also created a `devlog` skill for the `bruce/personal` Claude Code plugin to enforce the discipline
of updating this file after every work session. The skill gathers git context automatically and
follows the established entry format.

- Added `.java-version` pinned to 25.0 for jenv
- Created README.md with project overview, motivation, and reference links
- Created CLAUDE.md with full architecture documentation
- Built `devlog` skill (SKILL.md) for Claude Code personal plugin
- Verified full build passes on JDK 25.0.3 (Temurin LTS)

**Gotcha:** Claude Code cannot write to `.claude/settings.json` directly — the harness blocks
self-modification of permission files. Settings must be created manually or committed as part of
the repo.

**Tests:** 158 total (147 unit + 11 integration), 0 failures. No test changes this session.

---

## 2026-05-13 — Day 2 (continued): Feature Blitz & CI

### Session 4: Full Feature Implementation with Parallel TDD Agents (14:00–14:30)

**Commits:** `e23fdd1` → `e1a9ed7`

Massive feature session — implemented six major features in parallel using Claude Code agents with
git worktrees and direct-to-tree writes. The approach: research first (old spring-data-solr
limitations, current dependency versions), then dispatch TDD agents for each feature simultaneously.

**Features delivered:**

- **`@Field` name mapping** (`f021b65`) — `SolrFieldNameResolver` scans `@Field` annotations at
  class-load time, caches per entity type. `SolrQueryCreator` now resolves field names through it.
  The #1 pain point from the original spring-data-solr, fixed. 5 new tests.

- **`@Query` annotation** (`2e4cc93`) — `StringBasedSolrQuery` executes raw Solr query strings with
  `?0`, `?1` parameter substitution. `SolrQueryLookupStrategy` routes `@Query`-annotated methods to
  it, falls back to `PartTreeSolrQuery`. Added `count` attribute for count queries. Also added
  `count(String, SolrQuery)` overload to `SolrTemplate` — the `SimpleQuery` variant now delegates to
  it. 8 new tests.

- **Highlighting** (`8a513f4`) — `HighlightPage<T>`, `HighlightEntry<T>` record, `HighlightOptions`
  (pre/post tags, snippets, fragsize). `SolrTemplate.queryForHighlightPage()` pairs docs with
  highlight snippets by document ID. Uses `hl.tag.pre`/`hl.tag.post` (unified highlighter). ~36 new
  tests.

- **Faceting** (`c222225`) — `FacetPage<T>`, `FacetFieldEntry`/`FacetQueryEntry` records,
  `FacetOptions` (field facets, query facets, minCount, limit, sort). Parses SolrJ `FacetField` and
  facet query map from `QueryResponse`. Uses `FacetParams` constants. ~32 new tests.

- **Cursor-based deep paging** (`8c74635`) — `CursorResult<T>` record wrapping Solr's cursorMark.
  `hasMore` detects exhaustion when `nextCursorMark == requestCursorMark`. Documented the Solr
  requirement for sort to include uniqueKey field. ~11 new tests.

- **JaCoCo code coverage** (`22e3540`) — `jacoco-maven-plugin` 0.8.13 with 80% LINE coverage
  threshold on autoconfigure module. Starter and sample modules skip JaCoCo. Added 6 tests to
  SolrTemplateTest to cover previously untested branches (annotated collection methods, soft commit,
  commit mode immediate). Coverage at 81.7%.

**Infrastructure:**
- **GitHub Actions CI** (`272cfd9`) — `.github/workflows/ci.yml` triggers on push/PR to master. JDK
  25 Temurin, Maven cache, JaCoCo report upload as artifact.
- **README overhaul** — badges (CI, Java, Spring Boot, SolrJ, License), feature list, quick start
  guide, usage examples with code snippets.
- **LIMITATIONS.md** (`e23fdd1`) — comprehensive documentation of the archived spring-data-solr
  project's bugs, gaps, and what Lazarus improves.

**Sample app enhanced** (`e1a9ed7`) — DataLoader seeds 10 books on startup. BookRepository
demonstrates derived queries, `@Query` with parameter substitution, count queries. BookController
showcases CRUD, highlighting, faceting, cursor paging, and statistics endpoints.

**Gotchas discovered:**
- Parallel agents writing to the same file (SimpleQuery.java) required manual integration — three
  agents each added their feature (highlight/facet/cursor) but the last writer's version needed the
  other two features merged in
- `FacetParams` lives in `org.apache.solr.common.params.FacetParams`, not nested on `SolrQuery`
- Worktree agents can commit directly to the main branch — the worktree isolation prevents file
  conflicts but not branch conflicts. Merging worktree branches into master with concurrent
  direct-to-tree writes required stash/merge/pop choreography
- `mvn clean` fails on locked surefire-reports directory when agents hold file handles — use
  `mvn verify` without clean, or wait for agents to complete

**Tests:** 256 total (245 unit + 11 integration), 0 failures. JaCoCo coverage gate passes at >80%.

### Session 5: Multi-Version Integration Tests (14:30–14:45)

**Commit:** `a74f812`

Refactored integration tests to run against both Solr 9 and Solr 10. The original `SolrIntegrationTest`
used a hardcoded `solr:9` image — a leftover from the initial scaffolding session, never upgraded to
match the SolrJ 10 dependency. Rather than simply bumping to `solr:10`, took the smarter approach:
prove backward compatibility by testing both versions.

Extracted all test logic into `AbstractSolrIntegrationTest` (abstract base), then created two thin
concrete subclasses — `Solr9IntegrationTest` and `Solr10IntegrationTest` — each specifying only the
Docker image version. JUnit 5 discovers `@Nested` inner classes from superclasses, so all test
groupings (SaveAndRetrieve, QueryOperations, DeleteOperations, HealthIndicator) are inherited
automatically. Each subclass gets its own Testcontainers lifecycle.

Also consolidated the two `@BeforeEach` methods (`setUp` and `cleanCollection`) into a single method
to eliminate a latent ordering dependency — JUnit 5 does not guarantee execution order of multiple
`@BeforeEach` methods within the same class.

**Gotchas discovered:**
- JUnit 5 DOES discover `@Nested` inner classes declared in abstract superclasses — `findNestedClasses`
  traverses the full class hierarchy. This makes the abstract base + concrete subclass pattern viable
  for multi-version Testcontainers testing.
- `SolrContainer` in Testcontainers 2.0.5 is fully compatible with both `solr:9` and `solr:10` images.
  Version-aware logic (Zookeeper startup commands at 9.7.0+), collection creation, and Jetty startup
  detection all work unchanged across both versions.

**Tests:** 267 total (245 unit + 22 integration), 0 failures. JaCoCo coverage gate passes at >80%.

### Session 6: Polish, Fixes & Developer Experience (15:00–16:40)

**Commits:** `4932c4a` → `8cde7e9`

Housekeeping session — restored personality to the README after the Session 4 agents had steamrolled
Bruce's narrative intro with a generic one-liner. Merged both versions: Bruce's voice for the opening
hook and "Why?" motivation, the agent's technical depth for Features/Tech Stack/Quick Start. Added a
Prerequisites section and a Solr `9 | 10` compatibility badge.

Added the Maven Wrapper (`mvnw` / `mvnw.cmd`, Maven 3.9.15) so contributors can build without a local
Maven installation. All README build commands now reference `./mvnw` instead of `mvn`.

Added `paths-ignore` to the GitHub Actions CI workflow — pushes that only touch markdown, assets,
license, `.gitignore`, `.java-version`, or `.claude/` config no longer trigger a build.

Fixed a startup-crashing bug in the sample app: `@EnableSolrRepositories` was missing `includeFilters`,
`excludeFilters`, and `bootstrapMode` attributes. Spring Data Commons 4.0.5's
`AnnotationRepositoryConfigurationSource.hasExplicitFilters()` reads these via `getAnnotationArray()`
and throws `IllegalArgumentException` when they're absent. This was a latent bug — the unit and
integration tests never exercised the full auto-configuration boot path that the sample app triggers.

Deeper than initially expected — after fixing `includeFilters`/`excludeFilters`, two more startup
failures surfaced in sequence:

- `NoClassDefFoundError: InvalidDataAccessApiUsageException` — `spring-tx` is declared `<optional>true`
  in `spring-data-commons` 4.0.5, so it doesn't flow transitively. Added it explicitly to the starter
  POM.
- `NullPointerException: solrTemplate is null` in `SimpleSolrRepository.deleteAll()` —
  `SolrRepositoryConfigurationExtension` was missing a `postProcess()` override to wire `solrTemplate`
  into the `SolrRepositoryFactoryBean` via `addPropertyReference`. Without this, Spring Data creates the
  factory bean but never injects the template. Added both the generic `RepositoryConfigurationSource` and
  the `AnnotationRepositoryConfigurationSource` overloads (the latter reads `solrTemplateRef` from the
  annotation attributes).

The full `@EnableSolrRepositories` annotation now declares all attributes that
`AnnotationRepositoryConfigurationSource` reads: `repositoryFactoryBeanClass`, `queryLookupStrategy`,
`namedQueriesLocation`, `repositoryImplementationPostfix`, and `considerNestedRepositories` — in
addition to the earlier `includeFilters`, `excludeFilters`, and `bootstrapMode`. Attribute names were
extracted by decompiling `AnnotationRepositoryConfigurationSource` bytecode and scanning for `ldc`
string constants. The `fragmentsContributor` attribute is guarded by `containsKey` and can be omitted.

After all fixes: `Started SampleApplication in 2.8s`, `Loaded 10 sample books into Solr`. Full
end-to-end boot confirmed.

**Gotchas discovered:**
- Spring Data Commons 4.0.5 `AnnotationRepositoryConfigurationSource` reads 12+ attributes from the
  enable annotation via `getRequiredAttribute` — any missing attribute causes `IllegalArgumentException`
  at startup. The old spring-data-solr didn't need these because Spring Data Commons 3.x didn't read
  them in the same code path. This is undocumented in the Spring Data 4.0 migration guide.
- `spring-tx` is `<optional>true` in `spring-data-commons` — starters must pull it in explicitly.
- `RepositoryConfigurationExtensionSupport.postProcess()` is the wiring point for custom template
  references. Without it, the factory bean creates repositories with null collaborators. Unit tests
  don't catch this because they construct `SimpleSolrRepository` directly with constructor injection.
- Running `mvn spring-boot:run -pl solr-spring-boot-sample` from the repo root causes a Docker Compose
  file lookup failure because the working directory is the repo root, not the module directory. The
  Docker Compose support resolves paths relative to `user.dir`.

**Tests:** 267 total (245 unit + 22 integration), 0 failures.

### Session 7: Runtime Fixes — Compiler Flags & Schema Mapping (16:50–17:10)

**Commit:** `faaebcd`

Two more runtime errors surfaced when actually exercising the sample app's REST endpoints — the kind
of bugs that only appear when the full request path executes, not at startup.

First: `@RequestParam` and `@PathVariable` bindings failed with missing parameter names. Spring
Framework 7 (used by Boot 4.0.6) no longer infers parameter names from bytecode debug info — it
requires the `-parameters` compiler flag. Added `<parameters>true</parameters>` to the
`maven-compiler-plugin` configuration in the parent POM.

Second: Solr's default schemaless ("managed") mode auto-creates bare field names (e.g. `title`,
`author`) as `multiValued="true"`. When SolrJ deserialises the response, it returns `ArrayList`
instead of `String`, causing `IllegalArgumentException: Can not set String field to ArrayList`.
Fixed by switching all `@Field` annotations in the Book entity to Solr's dynamic field suffixes:
`title_s`, `author_s`, `year_i`, `price_d`, `genre_s`. Updated all `@Query` annotations in
`BookRepository` and raw field references in `BookController` (highlight, facet queries).

Also removed the explicit `file: docker-compose.yml` property from `application.yml` — Spring Boot's
Docker Compose auto-detection finds `docker-compose.yml` in the working directory without it.

After fixes: all sample app endpoints verified end-to-end — `/api/books` returns 10 books,
`/api/books/stats` returns correct counts, `/api/books/search?q=Spring` filters correctly.

**Gotchas discovered:**
- Spring Framework 7 requires `-parameters` for `@RequestParam`/`@PathVariable` name resolution —
  without it, parameter names are `arg0`, `arg1` and binding silently fails. This is a Boot 4.x
  migration gotcha not immediately obvious from compiler output.
- Solr's default managed schema creates bare field names as `multiValued="true"`. Use dynamic field
  suffixes (`_s`, `_i`, `_d`, `_l`, `_b`, `_dt`) to get single-valued fields in schemaless mode.
  Alternatively, define an explicit schema — but dynamic suffixes are the path of least resistance
  for a starter library sample.

**Tests:** 267 total (245 unit + 22 integration), 0 failures.

---

## What's Next

- [x] `@Query` annotation — raw Solr query strings on repository methods
- [x] `@Field` name mapping in derived queries
- [x] Faceting, highlighting support
- [x] README.md with usage documentation
- [x] GitHub Actions CI pipeline
- [x] JaCoCo code coverage
- [x] Document old spring-data-solr limitations
- [ ] Cursor-based deep paging integration tests (Testcontainers)
- [ ] Faceting/highlighting integration tests (Testcontainers)
- [ ] Custom converter pipeline (MappingSolrConverter)
- [ ] Geospatial query support (Near, Within)
- [ ] Streaming expressions
- [ ] Publish to Maven Central
