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

### Session 8: Package Rename — com.tomaytotomato.data.solr (17:20–17:50)

**Commit:** `a8f8651`

Renamed all Maven coordinates and Java packages to align with the `com.tomaytotomato` domain used
for published artifacts (e.g. `location4j`). The "Lazarus" branding stays in the repo name and
README but no longer leaks into import statements or POM coordinates.

**What changed:**
- groupId: `dev.solrlazarus` → `com.tomaytotomato`
- Parent artifactId: `spring-data-solr-lazarus` → `spring-data-solr`
- Java packages: `dev.solrlazarus.autoconfigure` → `com.tomaytotomato.data.solr`
- Sample app: `dev.solrlazarus.sample` → `com.tomaytotomato.data.solr.sample`
- Auto-configuration imports file updated to new FQCNs
- README dependency snippet updated

The package structure mirrors the original `org.springframework.data.solr` — just under our domain.
Module directory names (`solr-spring-boot-autoconfigure`, etc.) stayed the same since they never
contained "lazarus".

Also added a symlink at the repo root (`docker-compose.yml → solr-spring-boot-sample/docker-compose.yml`)
so Docker Compose commands work from either the repo root or the module directory.

71 files touched — all renames plus package declaration / import updates. Verified with `git mv` for
clean rename tracking in git history.

**Tests:** 267 total (245 unit + 22 integration), 0 failures. BUILD SUCCESS.

---

### Session 9: Pre-Release Code Reviews (18:00–18:30)

**No commits — review session only.**

Ran two simulated code reviews before considering public release — one from the perspective of Rod
Johnson (architectural rigour, Spring Data conventions) and one from Josh Long (developer experience,
demo-ability, ecosystem citizenship). Both reviewed every production and test source file.

**Key finding:** Both reviewers independently converged on the same top-5 issues — a strong signal
these are real problems, not stylistic preferences.

Full reviews saved in [`docs/reviews/`](docs/reviews/):
- [`rod-johnson-review.md`](docs/reviews/rod-johnson-review.md) — architectural review
- [`josh-long-review.md`](docs/reviews/josh-long-review.md) — developer experience review

**Consensus strengths:**
- Auto-configuration chain follows Boot conventions precisely
- Testcontainers dual-version testing pattern is excellent
- `@Field` name mapping in derived queries (the original's #1 pain point) is solid
- `CursorResult` record design is correct
- `SolrFieldNameResolver` thread-safe caching is well done

**On Solr 8 backwards compatibility:** Both said no. The project's premise is "the original died on
Solr 8; we start at 9." Teams on Solr 8 have the NET-A-PORTER fork. Adding backwards compatibility
would dilute the codebase and betray the narrative.

**Tests:** 267 total (245 unit + 22 integration), 0 failures. No test changes this session.

---

## What's Next

### Pre-Release: Must Fix — DONE

- [x] **Solr injection in `StringBasedSolrQuery.resolveParameters`** — escaped with
      `ClientUtils.escapeQueryChars()` (`2731de1`)
- [x] **`SolrEntityInformation.getId()` returns null** — `@Field`-based ID reflection implemented
      (`f330fd8`)
- [x] **`delete(entity)` / `deleteAll` throw `UnsupportedOperationException`** — implemented via
      `getId()` (`f330fd8`)
- [x] **`count(SolrQuery)` mutates caller's object** — defensive copy via `getCopy()` (`bfc945b`)
- [x] **`findAllById` — escape IDs** with `ClientUtils.escapeQueryChars()` (`2731de1`)

### Pre-Release: Should Fix — DONE

- [x] **Remove `getSolrClient()` from `SolrOperations` interface** — kept on `SolrTemplate` as
      concrete method (`bfc945b`)
- [x] **`@SolrDocument` placeholder resolution** — `SolrDocumentResolver` now accepts `Environment`
      (`54b3aa9`)
- [x] **`Criteria.contains/startsWith/endsWith` — escape inner value** before wrapping with
      wildcards (`2731de1`)
- [x] **`SolrFieldNameResolver` — walk class hierarchy** with `putIfAbsent` for subclass precedence
      (`bfc945b`)
- [x] **Add `spring.solr.commit-mode` to `additional-spring-configuration-metadata.json`**
      (`bfc945b`)
- [x] **Repository auto-configuration integration test** — context-level bean wiring verified
      (`54b3aa9`)

### Infrastructure — DONE

- [x] **JDK 21 baseline** — compiler target lowered from 25 to 21, no JDK 22+ features in codebase
      (`6e991a6`)
- [x] **CI matrix** — GitHub Actions now tests on JDK 21 and 25 (`6e991a6`)
- [x] **Dependabot** — weekly scans for Maven dependencies and GitHub Actions versions (`cbbfdac`)
- [x] **README** — SolrCloud/standalone/custom-client configuration documented (`2330300`)

### Session 10: Six Features in Parallel + Coverage Gap Fixes (19:30–20:15)

**Commits:** `19b4416` → `2b22155` (features), then coverage tests on master

Dispatched six worktree agents in parallel to implement the remaining post-release enhancements.
All six completed, merged into master, and passed a unified build. Then closed JaCoCo coverage
gaps flagged in the report — `SolrTemplate.savePartialUpdate` (completely untested),
`CloudSolrClientConfiguration` (0% covered), and missing branch coverage across highlight/facet/
streaming paths.

**Features delivered (parallel agents):**

- **Micrometer instrumentation** (`19b4416`) — `MicrometerSolrTemplate` extends `SolrTemplate`,
  wraps 10 operations with `Timer.record()`. Metric name `solr.operations`, tags `operation` +
  `collection`. Auto-configured via inner `@Configuration` gated on `@ConditionalOnClass` +
  `@ConditionalOnBean(MeterRegistry.class)`. `micrometer-core` is `<optional>true</optional>`.
  20 tests.

- **Cursor/facet/highlight integration tests** (`53cceb1`) — 7 new Testcontainers tests in
  `AbstractSolrIntegrationTest`. Cursor paging: first page, second page different IDs, full
  traversal. Faceting: field facets with author counts, query facets with year ranges.
  Highlighting: snippet presence with `<em>` tags, field key verification. Run on both Solr 9
  and 10.

- **Custom converter pipeline** (`894739c`) — Foundation layer: `SolrReadConverter`,
  `SolrWriteConverter` functional interfaces, `SolrCustomConversions` immutable registry,
  `SolrMappingConverter` wrapper. Auto-configured with `@ConditionalOnMissingBean` for user
  override. Converter *application* during read/write is a deliberate follow-up. 12 tests.

- **Geospatial query support** (`08812d8`) — `GeoPoint` and `GeoDistance` records with unit
  conversion (km/miles). `Criteria.near()` emits `{!geofilt}`, `Criteria.within()` emits
  `{!bbox}`. `SolrQueryCreator` handles `NEAR` and `WITHIN` Part.Types. Raw predicates
  (no `field:` prefix) handled via a `raw` flag on `Predicate`. 13 tests.

- **SolrProperties constructor binding** (`06d3620`) — All fields now `final`, single constructor
  with `@DefaultValue` annotations. No setters. Boot 4 auto-detects constructor binding from the
  single-constructor rule. **CloudSolrClient timeout wiring** — `HttpJdkSolrClient.Builder` (which
  extends `HttpSolrClientBuilderBase`) carries the timeouts, passed via `withHttpClientBuilder()`.
  Existing SolrProperties tests passed without modification.

- **Streaming expressions** (`8fcd0ea`) — `StreamingExpression` fluent builder with `of(raw)` and
  `search(collection)` entry points. `SolrTemplate.stream()` posts to `/stream` via
  `GenericSolrRequest` with `SolrRequestType.STREAMING`. Response parser handles NamedList
  unwrapping and EOF tuple skipping. 14 tests.

**Merge choreography:** Stashed uncommitted DEVLOG, merged Micrometer (fast-forward), integration
tests, then converter pipeline (conflict in `SolrAutoConfiguration` — both Micrometer and converter
added inner configuration classes + imports; resolved by keeping both). Geospatial, SolrProperties,
and streaming merged cleanly. Six merge commits total.

**Coverage gap fixes (on master after merge):**

- `SavePartialUpdate` — 3 tests: verifies `SolrInputDocument` field mapping (id, set, increment),
  `IOException` wrapping, `SolrServerException` wrapping
- `CommitModeImmediate` — 4 new tests: `saveAll`, `savePartialUpdate`, `deleteById`, `deleteByQuery`
  all commit when IMMEDIATE (previously only `save` was tested)
- `QueryForPageWithAnnotatedCollection` — verifies `@SolrDocument` collection resolution in the
  `Pageable` overload
- `QueryForHighlightPage` null docId branch — verifies empty highlights when Solr document has no
  `id` field value
- `Stream` non-List docs — verifies graceful empty result when `docs` is not a `List`
- `Count` SimpleQuery overload — verifies delegation and error wrapping
- `CloudSolrClientConditionalConfiguration` — 3 tests: standard client when no `zk-host`, user
  client precedence when `zk-host` set, context failure when `zk-host` set without real ZK

**Coverage improvement:**

| Class | Before | After |
|-------|--------|-------|
| SolrTemplate lines | 92% (13 missed) | 99% (2 missed) |
| CloudSolrClientConfiguration lines | 0% (10 missed) | 90% (1 missed) |

Remaining misses are catch blocks for exception variants already tested through the `IOException`
path — same wrapping pattern, diminishing returns.

**Gotchas discovered:**
- `CloudSolrClient.Builder.build()` eagerly connects to ZooKeeper — cannot unit test actual bean
  creation without a running cluster. Testcontainers integration tests cover this path. Unit tests
  verify the conditional property activation and user-bean back-off behaviour.
- Six parallel worktree agents that modify the same file (`SolrAutoConfiguration`) will always
  conflict at merge time. Predictable and fast to resolve, but ordering matters — merge the
  simplest diff first to minimise conflict surface for later merges.

**Tests:** 397 total (363 unit + 34 integration), 0 failures. JaCoCo coverage gate passes.

---

## What's Next

### Post-Release: Enhancements — DONE

- [x] **Micrometer instrumentation** — `MicrometerSolrTemplate` with Timer wrapping (`19b4416`)
- [x] **`SolrProperties` constructor binding** — final fields, `@DefaultValue`, no setters
      (`06d3620`)
- [x] **CloudSolrClient timeout wiring** — via `withHttpClientBuilder()` (`06d3620`)
- [x] **Cursor/facet/highlight integration tests** — 7 Testcontainers tests (`53cceb1`)
- [x] **Custom converter pipeline** — foundation layer with auto-configuration (`894739c`)
- [x] **Geospatial query support** — `GeoPoint`, `GeoDistance`, `near()`, `within()` (`08812d8`)
- [x] **Streaming expressions** — fluent builder + `SolrTemplate.stream()` (`8fcd0ea`)

---

## 2026-05-14 — Day 3: Sample App Overhaul & Data Curation Pipeline

### Session 1: Curated Dataset & Full Feature Showcase (09:00–10:10)

**Commits:** (uncommitted — pending ship)

Built a Java-based data curation pipeline and overhauled the sample app to demonstrate every feature
of the starter with realistic data. No Python — the entire pipeline is a new Maven module.

**New module: `solr-spring-boot-data-curator`**

A standalone Java pipeline that transforms a 7k-book Kaggle CSV into a curated 250-book JSON dataset:

- `BookCurator` — main pipeline: read CSV → filter (description≥50 chars, year>0, rating>0) →
  diversify by genre (round-robin allocation: `TARGET_TOTAL / genreCount` per genre, remainder to
  largest) → enrich with geo/pricing → write JSON
- `CategoryNormaliser` — regex-based genre mapping from messy LC subject headings to clean categories.
  Order matters: specific compound genres (True Crime, Science Fiction, Historical Fiction) must
  precede general keyword patterns, otherwise "True Crime" matches "Mystery" via the `crime` pattern
- `BookshopLocations` — maps 17 genre categories to real-world bookshop coordinates (lat/lon/name)
  for geospatial enrichment. Uses `Map.ofEntries()` for fail-fast on duplicate keys
- `PricingStrategy` — category-specific price ranges with page count and rating multipliers
- `CsvBookRecord` / `CuratedBook` — Jackson records for CSV parsing and JSON output

Run with: `./mvnw compile exec:java -pl solr-spring-boot-data-curator`

**Sample app overhaul:**

Expanded `Book` entity from 10 to 15 fields — added subtitle, ratingsCount, location
(LatLonPointSpatialField), locationName, and `@Score` for relevance scoring. `DataLoader` now reads
from `curated-books.json` using Jackson 3.x (`tools.jackson` packages). `BookRepository` expanded
with 12 derived query methods plus `@Query` annotations. `BookController` showcases every starter
feature: CRUD, derived queries, `@Query` methods, highlighting (description + title), faceting
(categories + author), cursor-based deep paging, geospatial (near/within), partial updates
(set price, add category), and stats.

Added custom `managed-schema.xml` with `LatLonPointSpatialField` for the `location` field, mounted
into Docker Compose Solr container.

**Integration tests:** 17 Testcontainers tests across 8 nested groups — DataLoading (count, field
population), DerivedQueries (titleContaining, titleStartingWith, inStock, priceBetween, rating,
yearBetween, countByAuthor, existsByTitle), CustomQueries, Highlighting, Faceting, CursorPaging,
CrudOperations, and PartialUpdates.

**Gotchas discovered:**
- **SolrJ BindingException with schemaless Solr:** Testcontainers `SolrContainer` creates collections
  using the `_default` configset (schemaless mode), NOT our custom `managed-schema.xml`. When the
  `location` field is indexed as `"40.7484,-73.9856"`, schemaless mode auto-detects it as a
  multiValued string. SolrJ then returns `ArrayList` instead of `String`, causing
  `IllegalArgumentException: Can not set String field to ArrayList`. Fixed by pre-defining the field
  via Solr's Schema API (HTTP POST to `/solr/books/schema`) in `@DynamicPropertySource`, which runs
  after container start but before Spring context loads. The `_default` configset already has a
  `location` field TYPE (LatLonPointSpatialField) — we just need to define the FIELD before data
  indexing begins.
- **Text analysis vs string matching in tests:** `findByTitleStartingWith("The")` on a `text_general`
  field matches any document where a token starts with "the" (after lowercasing), not titles
  beginning with "The". Similarly, `existsByTitle("This Title Surely Does Not Exist XYZ123")` returns
  `true` because individual tokens ("title", "exist") match real book titles after analysis. Derived
  query methods delegate to Solr's query parser, which respects the field's analyzer chain.
- **Default pagination:** Solr returns 10 rows by default. `List<T>`-returning repository methods
  don't override this, so `findByInStock(true).size() + findByInStock(false).size()` returns 20, not
  250.
- **Jackson 3.x packages in Spring Boot 4:** `DataLoader` must import `tools.jackson.core.type.TypeReference`
  and `tools.jackson.databind.ObjectMapper`, not the `com.fasterxml.jackson` packages. Spring Boot
  4.0.6 bundles Jackson 3.x.
- **Kaggle CSV category fragmentation:** Categories are single LC subject headings with internal
  commas (e.g., "Authors, English"). Splitting on commas shreds them into meaningless fragments.
  Regex-based pattern matching is the correct approach.
- **exec:java path resolution:** `exec-maven-plugin` resolves relative paths from the repository
  root, not the module directory. Default paths must be prefixed with the module directory name.

**Tests:** 414 total (363 unit + 51 integration), 0 failures. JaCoCo coverage gate passes at >80%.

---

## 2026-05-14 — Day 3 (continued): Custom Document Mapping Layer

### Session 2: SolrDocumentReader — Bypass SolrJ's DocumentObjectBinder (14:30–15:40)

**Commit:** `2f42b41`

The `getAll` endpoint in the sample app threw `IllegalArgumentException: Can not set String field
to ArrayList` — SolrJ's `DocumentObjectBinder` can't handle the type mismatch when
`LatLonPointSpatialField` with `docValues="true"` returns `ArrayList<Double>` instead of
the `String` that was indexed. Rather than patching the schema or the entity, we built what the
original spring-data-solr always had: a custom document mapping layer that sits between Solr
responses and Java entities.

Designed a single generic interface `SolrDocumentConverter<S, T>` with a `convert()` method, then
two concrete implementations — `SolrDocumentReader<T>` (SolrDocument → entity) and
`SolrDocumentWriter<T>` (entity → SolrInputDocument, stub for now). This replaced the unused
`SolrReadConverter` and `SolrWriteConverter` marker interfaces from the v0.1 converter pipeline
scaffolding.

`SolrDocumentReader` uses reflection to walk `@Field`-annotated fields (including superclass
hierarchy), resolve Solr field names from annotation values, and apply a type coercion chain:
direct assignment → single-element collection unwrap → `ArrayList<Double>` to comma-joined `String`
(the spatial field fix) → multi-valued field to `List`. Also handles `@Score` mapping.

Wired into `SolrTemplate` by replacing all six `response.getBeans(type)` calls with
`mapDocuments(response.getResults(), type)` using the new reader. Removed the `SolrCustomConversions`
field from `SolrTemplate` (it was never applied). Updated all test fixtures across `SolrTemplateTest`
and `MicrometerSolrTemplateTest` to use real `SolrDocument` objects instead of mocking `getBeans()`.

Also produced `SOLR-FIELD-TYPES.md` — a comprehensive reference documenting Java type mappings for
all Solr 9/10 field types, the `DocsStreamer.getValue()` dispatch chain, and the `KNOWN_TYPES` set
that determines whether `toObject()` or `toExternal()` is called. Key finding: spatial types are NOT
in KNOWN_TYPES, which is why the docValues path returns different types than the stored field path.

**Gotchas discovered:**
- SolrJ's `DocumentObjectBinder` performs strict type matching with zero coercion — `ArrayList<Double>`
  to `String` is simply not supported, despite Solr accepting `String` on write and returning
  `ArrayList` on read for the same spatial field
- `DocsStreamer.getValue()` has a two-path dispatch: if the field type is in `KNOWN_TYPES`, it calls
  `toObject()` (typed); otherwise `toExternal()` (String). Spatial types aren't in `KNOWN_TYPES`.
  The docValues code path is entirely separate and can return different Java types
- Non-public inner test classes (`static class TestDocument`) fail with `IllegalAccessException` when
  instantiated via `getDeclaredConstructor().newInstance()` — fixed by making test fixtures `public`
  rather than using `constructor.setAccessible(true)` (SonarQube flagged the reflection approach)
- SolrJ's `@Field` annotation uses `"#default"` as the sentinel for "use the Java field name" — must
  check for both empty string and `"#default"`

**Tests:** 401 total (367 unit + 34 integration), 0 failures. 4 new reader tests + fixture updates
across 4 existing test files.

### Session 3: SolrDocumentWriter — Complete the Mapping Layer (PR #9)

**Commit:** `7a8d812` (merged via PR #9, `ac11caf`)

Closed the write-side gap left by Session 2. `SolrDocumentWriter` mirrors `SolrDocumentReader` — walks
`@Field`-annotated fields (including superclass hierarchy), resolves Solr field names from annotation
values, and converts entity → `SolrInputDocument`. `SolrTemplate.save()` and `saveAll()` now bypass
SolrJ's `DocumentObjectBinder` entirely, using the writer for symmetric round-trip semantics with the
reader.

This completes the custom document mapping layer that was the original `spring-data-solr`'s strongest
feature — and the one most teams missed when forced to fall back to raw SolrJ after archival.

**Tests:** SolrDocumentWriter test class added, existing SolrTemplate save tests adapted. Build green.

### Session 4: Docs Reshuffle, Architecture Diagrams, Dependency Hygiene (later in the day)

**Commits:** `13f8599` (image move), `38b60b7`, `1406a21`, `fc3480e`, `262e987`, `027c24d` (README polish),
plus Dependabot merges `eea738f`, `940e071`, `20eff35`, `009737e`, `2280565`.

Docs housekeeping: moved `DEVLOG.md`, `LIMITATIONS.md`, and `SOLR-FIELD-TYPES.md` under `docs/`,
restructured the README feature list into a table, fixed assorted spelling, moved the title image
into `assets/`. Three README links to the moved docs were stale (`README.md:63`, `:176`, `:177`) —
fixed in this same session.

Added `docs/ARCHITECTURE.md` with four Mermaid diagrams covering the auto-configuration chain, the
repository-call sequence flow, the Spring Boot middleware integration map, and the configuration
surface. Renders inline on GitHub.

Dependabot merges absorbed cleanly: jacoco 0.8.13 → 0.8.14, surefire 3.5.5, compiler-plugin 3.15.0,
`actions/setup-java` → 5, `actions/upload-artifact` → 7, `actions/checkout` → 6.

**Tests:** 401 total (367 unit + 34 integration), unchanged. No production code touched.

---

## What's Next

### Remaining

- [ ] Publish to Maven Central
- [x] Converter application during read (SolrDocumentReader wired into SolrTemplate, Day 3 Session 2)
- [x] Converter application during write (SolrDocumentWriter, Day 3 Session 3, PR #9)
- [ ] `@Highlight` and `@Facet` method-level annotations for repository methods
- [ ] Geospatial integration tests with Testcontainers (requires spatial field type in schema)
- [ ] Streaming expressions integration tests (requires streaming handler enabled in Solr config)
