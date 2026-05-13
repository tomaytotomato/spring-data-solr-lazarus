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

---

## What's Next

- [ ] `@Query` annotation — raw Solr query strings on repository methods
- [ ] `@Field` name mapping in derived queries
- [ ] Faceting, highlighting, grouping support
- [ ] Custom converter pipeline (MappingSolrConverter)
- [ ] README.md with usage documentation
- [ ] Publish to Maven Central
