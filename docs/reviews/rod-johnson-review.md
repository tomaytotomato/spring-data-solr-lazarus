# Architectural Review — Rod Johnson

**Date:** 2026-05-13
**Reviewer:** Rod Johnson (simulated) — creator of the Spring Framework
**Version reviewed:** 0.1.0-SNAPSHOT (commit `584f438`)

---

## Overall Verdict

**Needs changes before public release — but the foundation is genuinely solid.**

This is a well-structured, cleanly implemented Spring Boot starter that correctly uses Spring Data
Commons' extension points and Boot's auto-configuration conventions. It is substantially more
complete than I expected for a 0.1.0 artefact. However, several issues would cause real pain for
production adopters, and one design decision in the core template interface is, in my view, wrong.

---

## Critical Issues (Must Fix)

### 1. Solr injection in `StringBasedSolrQuery.resolveParameters`

- **File:** `StringBasedSolrQuery.java:44-48`
- **Problem:** Parameter substitution uses `String.replace("?" + i, String.valueOf(parameters[i]))`.
  If a caller passes a value such as `") OR (*:*` as a parameter, the raw string goes straight into
  the Solr query unescaped. This is the Lucene/Solr equivalent of SQL injection. The test suite
  validates the happy path only.
- **Solution:** Apply `ClientUtils.escapeQueryChars()` to each substituted value, or provide a mode
  flag on `@Query` that opts in to raw (expert) mode — the same decision JPA's `@Query` made with
  `nativeQuery = true`. The default must be safe.

### 2. `SolrEntityInformation.getId()` returns `null` unconditionally

- **File:** `SolrEntityInformation.java:26`
- **Problem:** This method is part of `EntityInformation` and is called by Spring Data Commons
  infrastructure during lifecycle operations including auditing hooks and event publishing. Combined
  with `isNew()` returning `false` unconditionally, the entire new/existing entity distinction is
  broken. Spring Data Elasticsearch, for comparison, uses reflection to find the `@Id`-annotated
  field.
- **Solution:** Scan the entity class for SolrJ's `@Field` annotation on a field named `id`, or
  introduce a companion `@Id` annotation. `SolrFieldNameResolver` already demonstrates the pattern.

---

## Major Issues (Should Fix)

### 3. `SolrOperations` interface exposes `SolrClient` directly

- **File:** `SolrOperations.java:51`
- **Problem:** `SolrClient getSolrClient()` on the public operations interface destroys the
  abstraction. The whole point of the template pattern is to give callers a stable, testable API that
  does not require them to know anything about the underlying client. Exposing it means every test
  mock must handle it, every alternative implementation must provide it, and callers who use it are
  permanently coupled to SolrJ's client hierarchy.
- **Solution:** Remove `getSolrClient()` from `SolrOperations`. If the health indicator needs a
  client reference, obtain it from the application context directly.

### 4. `SimpleSolrRepository.findAllById` — no escaping, no row limit

- **File:** `SimpleSolrRepository.java:57`
- **Problem:** Builds `"id:(" + String.join(" OR ", idList) + ")"` with no escaping. If any ID
  contains Lucene special characters, the query is malformed. Given IDs are often UUIDs or natural
  keys, this is a real risk.
- **Solution:** Apply `ClientUtils.escapeQueryChars()` to each ID. Document the row-limit behaviour.

### 5. `count()` mutates the caller's `SolrQuery`

- **File:** `SolrTemplate.java:200-207`
- **Problem:** `count(String, SolrQuery)` calls `query.setRows(0)` on the object passed in. If a
  caller constructs a `SolrQuery`, passes it to `count()`, then reuses it, they get zero rows. The
  `count(SimpleQuery)` overload is safe because `toSolrQuery()` creates a new instance, but the raw
  overload is not.
- **Solution:** Defensive copy: `var countQuery = query.getCopy(); countQuery.setRows(0);`

### 6. `delete(entity)` / `deleteAll(entities)` throw `UnsupportedOperationException`

- **File:** `SimpleSolrRepository.java:80, 91`
- **Problem:** The `CrudRepository` contract requires these methods. Throwing
  `UnsupportedOperationException` is a Liskov substitution violation. Any framework component that
  invokes these through the `CrudRepository` interface — Spring Batch item writers, Spring Data REST,
  application code — will get a runtime exception with no compile-time warning.
- **Solution:** Implement `getId()` in `SolrEntityInformation` (Issue 2), then delegate
  `delete(entity)` to `deleteById(getId(entity))`.

---

## Minor Issues

- **Crowded root package** — `SolrOperations`, `SolrTemplate`, `SolrProperties`, and all result
  types sit in the root `com.tomaytotomato.data.solr` package. Spring Data Elasticsearch splits
  these across `core`, `client`, and `domain` subpackages. Consider for 0.2.0, because moving types
  post-release requires a deprecation cycle.
- **`commit-mode` missing from `additional-spring-configuration-metadata.json`** — IDE tooling won't
  offer completion or documentation.
- **`Criteria.contains()` doesn't escape the wildcard-wrapped value** — `contains("spring(boot")`
  produces `*spring(boot*`, which is malformed Lucene. The `escape()` method exists and should be
  used.
- **`SolrFieldNameResolver` doesn't walk the class hierarchy** — `getDeclaredFields()` only returns
  fields on the exact class. Inherited fields are invisible.

---

## What's Done Well

The auto-configuration structure follows Boot conventions precisely. `@ConditionalOnMissingBean` on
both `SolrClient` and `SolrTemplate` means users can provide their own beans. The
`CloudSolrClientConfiguration` inner static class with `@ConditionalOnProperty` prevents eager
construction when no ZooKeeper host is configured.

The Testcontainers integration test design is excellent — `AbstractSolrIntegrationTest` /
`Solr9IntegrationTest` / `Solr10IntegrationTest` is exactly how multi-version testing should be done.

`SolrFieldNameResolver`'s `ConcurrentHashMap` with `computeIfAbsent` is thread-safe and avoids
repeated reflection at query time.

The `CursorResult` record with `hasMore` derived from cursor equality is precisely correct.

**Verdict:** Not ready for Maven Central as-is. Fix the injection vulnerability, the mutating
`count()`, the broken `CrudRepository` contract, and remove `getSolrClient()` from the interface.
Then this is a legitimate 0.1.0.
