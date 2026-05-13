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
