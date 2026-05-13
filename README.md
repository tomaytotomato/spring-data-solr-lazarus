![title.png](assets/title.png)

# Spring Data Solr - Lazarus

[![CI](https://github.com/tomaytotomato/spring-data-solr-lazarus/actions/workflows/ci.yml/badge.svg)](https://github.com/tomaytotomato/spring-data-solr-lazarus/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![SolrJ](https://img.shields.io/badge/SolrJ-10.0.0-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

A modern Spring Boot starter for Apache Solr 10, resurrecting the archived [spring-data-solr](https://github.com/spring-attic/spring-data-solr) project with a clean reimplementation on a modern stack.

## Features

- **Auto-configuration** — drop-in `SolrClient` (standalone or SolrCloud) and `SolrTemplate` beans
- **Spring Data repositories** — `SolrRepository` with full CRUD, pagination, and sorting
- **Derived query methods** — 18 keywords (Is, Containing, Between, GreaterThan, In, IsNull, True, etc.)
- **`@Field` name mapping** — derived queries respect SolrJ `@Field` annotations (the #1 pain point from the original, fixed from day one)
- **`@Query` annotation** — raw Solr query strings with `?0`, `?1` parameter substitution
- **Highlighting** — `HighlightPage` with configurable pre/post tags, snippets, and fragment size
- **Faceting** — `FacetPage` with field facets, query facets, min count, and limit control
- **Cursor-based deep paging** — `CursorResult` wrapping Solr's cursorMark for efficient deep iteration
- **Partial updates** — atomic set/add/increment operations via `PartialUpdate`
- **Health indicator** — Spring Boot Actuator integration, pings collection or falls back to admin info
- **Criteria API** — fluent query builder: `Criteria.where("title").contains("spring").and("price").greaterThan(10)`

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Spring Boot | 4.0.6 |
| Spring Framework | 7.0.x |
| SolrJ | 10.0.0 |
| Testcontainers | 2.0.5 |

## Why?

![ytho.png](assets/ytho.png)

The original Spring Data Solr was [discontinued in April 2020](https://spring.io/blog/2020/04/07/spring-data-for-apache-solr-discontinued/) and [archived in September 2023](https://github.com/spring-attic/spring-data-solr). It never supported Solr 9+, Spring Boot 3+, or JDK 17+. Teams with Solr infrastructure were left with no Spring Data abstraction and no upgrade path.

Lazarus is a clean reimplementation — not a fork — fixing the original's design flaws and targeting the current stack. See [LIMITATIONS.md](LIMITATIONS.md) for a detailed comparison.

LLMs are pretty good at taking up the boring and painful rewriting tasks; as long as they are briefed and controlled properly. The hypothesis here is that creating a repo and having an LLM custodian will allow it to last longer and still have useful features added and maintained.

## Module Structure

```
solr-spring-boot-autoconfigure  — all the real code: auto-configuration, template, queries, repos
solr-spring-boot-starter        — thin POM that consumers depend on
solr-spring-boot-sample         — demo Spring Boot app with Docker Compose support
```

## Quick Start

### Build

```bash
mvn clean verify                    # full build + tests + coverage
mvn test                            # unit tests only
mvn test -pl solr-spring-boot-autoconfigure  # tests in autoconfigure module only
```

### Run the Sample App

With Docker running:

```bash
mvn spring-boot:run -pl solr-spring-boot-sample
```

This auto-starts a Solr 10 container with a pre-created `books` collection. The app exposes book CRUD endpoints and Actuator health at `/actuator/health`.

### Use in Your Project

Add the starter dependency (once published to Maven Central):

```xml
<dependency>
  <groupId>dev.solrlazarus</groupId>
  <artifactId>solr-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Configure in `application.yml`:

```yaml
spring:
  solr:
    host: http://localhost:8983/solr
    default-collection: myCollection
```

Define a repository:

```java
@SolrDocument(collection = "books")
public class Book {
  @Field String id;
  @Field String title;
  @Field String author;
}

public interface BookRepository extends SolrRepository<Book, String> {
  List<Book> findByAuthor(String author);
  List<Book> findByTitleContaining(String keyword);
  long countByAuthor(String author);
  
  @Query("title:?0 AND author:?1")
  List<Book> findByTitleAndAuthorCustom(String title, String author);
}
```

## Links and References

- [Dev Log](DEVLOG.md)
- [Limitations of the Original](LIMITATIONS.md)
- [Original Project (Spring Attic)](https://github.com/spring-attic/spring-data-solr)
- [Spring Boot Starter Template](https://github.com/ericus20/spring-boot-starter)
- [Apache SolrJ Reference](https://solr.apache.org/guide/solr/latest/deployment-guide/solrj.html)
- [A picture of Josh Long](https://joshlong.com/img/josh-hero-image.2ac6dba0.png)