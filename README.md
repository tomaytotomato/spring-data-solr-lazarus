![title.png](assets/title.png)

# Spring Data Solr - Lazarus

[![CI](https://github.com/tomaytotomato/spring-data-solr-lazarus/actions/workflows/ci.yml/badge.svg)](https://github.com/tomaytotomato/spring-data-solr-lazarus/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![Solr](https://img.shields.io/badge/Solr-9%20%7C%2010-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

Sometimes you wonder if a project is really dead or is just taking a break in the attic.

Like Lazarus, the [Spring Data Solr](https://github.com/spring-attic/spring-data-solr) project has
been resurrected from the bit void.

Rebuilt from scratch using Claude code with some analysis and human guidance.

Integration-tested against both Solr 9 and Solr 10.

## Features

- **Auto-configuration** : drop-in `SolrClient` (standalone or SolrCloud) and `SolrTemplate` beans
- **Spring Data repositories** : `SolrRepository` with full CRUD, pagination, and sorting
- **Derived query methods** : 18 keywords (Is, Containing, Between, GreaterThan, In, IsNull, True,
  etc.)
- **`@Field` name mapping** : derived queries respect SolrJ `@Field` annotations (the #1 pain point
  from the original, fixed from day one)
- **`@Query` annotation** : raw Solr query strings with `?0`, `?1` parameter substitution
- **Highlighting** : `HighlightPage` with configurable pre/post tags, snippets, and fragment size
- **Faceting** : `FacetPage` with field facets, query facets, min count, and limit control
- **Cursor-based deep paging** : `CursorResult` wrapping Solr's cursorMark for efficient deep
  iteration
- **Partial updates** : atomic set/add/increment operations via `PartialUpdate`
- **Health indicator** : Spring Boot Actuator integration, pings collection or falls back to admin
  info
- **Criteria API** : fluent query builder:
  `Criteria.where("title").contains("spring").and("price").greaterThan(10)`

## Tech Stack

| Component          | Version   |
|--------------------|-----------|
| Java               | 21+       |
| Spring Boot        | 4.0.6     |
| Spring Framework   | 7.0.x     |
| SolrJ              | 10.0.0    |
| Solr compatibility | 9.x, 10.x |
| Testcontainers     | 2.0.5     |

## Why?

![ytho.png](assets/ytho.png)

Projects often go stale or die because of burnout, lack of time/resources or changing priorities.

What if we could prevent this from happening by using LLMs to take on the maintenance burden? The
gaps in API drift would be managed, old dependencies and security issues would be resolved, and the
project would stay alive and useful for the community.

The original Spring Data Solr
was [discontinued in April 2020](https://spring.io/blog/2020/04/07/spring-data-for-apache-solr-discontinued/)
and [archived in September 2023](https://github.com/spring-attic/spring-data-solr). 

It never supported Solr 9+, Spring Boot 3+, or JDK 17+. Teams with Solr infrastructure were left with no
Spring Data abstraction and no upgrade path.

Lazarus is a clean reimplementation (not a fork), fixing the original's limitations.

See [LIMITATIONS.md](LIMITATIONS.md) for a detailed comparison.

## Module Structure

```
solr-spring-boot-autoconfigure  : all the real code: auto-configuration, template, queries, repos
solr-spring-boot-starter        : thin POM that consumers depend on
solr-spring-boot-sample         : demo Spring Boot app with Docker Compose support
```

## Quick Start

### Prerequisites

- JDK 21+ (or use [jenv](https://www.jenv.be/) — `.java-version` is included). CI tests on 21 and 25.
- Docker (for integration tests and the sample app)
- No Maven installation needed : the project includes the Maven Wrapper

### Build

```bash
./mvnw clean verify                    # full build + tests + coverage
./mvnw test                            # unit tests only
./mvnw test -pl solr-spring-boot-autoconfigure  # tests in autoconfigure module only
```

### Run the Sample App

With Docker running:

```bash
./mvnw spring-boot:run -pl solr-spring-boot-sample
```

This auto-starts a Solr 10 container with a pre-created `books` collection. The app exposes book
CRUD endpoints and Actuator health at `/actuator/health`.

### Use in Your Project

Add the starter dependency (once published to Maven Central):

```xml

<dependency>
  <groupId>com.tomaytotomato</groupId>
  <artifactId>solr-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Configure in `application.yml`:

```yaml
# Standalone Solr (default — uses HttpJdkSolrClient, zero extra dependencies)
spring:
  solr:
    host: http://localhost:8983/solr
    default-collection: myCollection
    connection-timeout: 10s
    request-timeout: 60s
    commit-mode: none          # or 'immediate' to hard-commit after each write
```

```yaml
# SolrCloud — activates automatically when zk-host is present (uses CloudSolrClient)
spring:
  solr:
    zk-host: zk1:2181,zk2:2181,zk3:2181
    default-collection: myCollection
```

If you need full control (custom TLS, `HttpJettySolrClient`, etc.), define your own `SolrClient`
bean and the auto-configuration backs off entirely.

Define a repository:

```java

@SolrDocument(collection = "books")
public class Book {

  @Field
  String id;
  @Field
  String title;
  @Field
  String author;
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

- [A picture of Josh Long](https://joshlong.com/img/josh-hero-image.2ac6dba0.png)
- [Dev Log](DEVLOG.md)
- [Limitations of the Original](LIMITATIONS.md)
- [Original Project (Spring Attic)](https://github.com/spring-attic/spring-data-solr)
- [Spring Boot Starter Template](https://github.com/ericus20/spring-boot-starter)
- [Apache SolrJ Reference](https://solr.apache.org/guide/solr/latest/deployment-guide/solrj.html)