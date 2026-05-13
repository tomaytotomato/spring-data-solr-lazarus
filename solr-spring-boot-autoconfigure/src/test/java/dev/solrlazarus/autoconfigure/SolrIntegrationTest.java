package dev.solrlazarus.autoconfigure;

import dev.solrlazarus.autoconfigure.health.SolrHealthIndicator;
import dev.solrlazarus.autoconfigure.query.Criteria;
import dev.solrlazarus.autoconfigure.query.SimpleQuery;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class SolrIntegrationTest {

  private static final String COLLECTION = "books";

  @Container
  static final SolrContainer solr = new SolrContainer(DockerImageName.parse("solr:9"))
      .withCollection(COLLECTION);

  private ApplicationContextRunner contextRunner;

  @BeforeEach
  void setUp() {
    contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class))
        .withPropertyValues(
            "spring.solr.host=http://" + solr.getHost() + ":" + solr.getSolrPort() + "/solr",
            "spring.solr.default-collection=" + COLLECTION,
            "spring.solr.commit-mode=IMMEDIATE"
        );
  }

  /**
   * Removes all documents from the collection after each test so tests do not bleed into each other.
   * Uses a dedicated template rather than relying on the context runner to avoid lifecycle issues.
   */
  @BeforeEach
  void cleanCollection() {
    contextRunner.run(ctx -> {
      var template = ctx.getBean(SolrTemplate.class);
      template.deleteByQuery(COLLECTION, "*:*");
      template.commit(COLLECTION);
    });
  }

  static TestBook book(String id, String title, String author, int year) {
    var book = new TestBook();
    book.id = id;
    book.title = title;
    book.author = author;
    book.year = year;
    return book;
  }

  public static class TestBook {
    @Field
    String id;

    @Field("title_s")
    String title;

    @Field("author_s")
    String author;

    @Field("year_i")
    int year;
  }

  @Nested
  class SaveAndRetrieve {

    @Test
    void savesDocumentAndRetrievesItByIdWithAllFields() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        var picard = book("1", "Make It So", "Jean-Luc Picard", 2350);

        template.save(COLLECTION, picard);

        var found = template.findById(COLLECTION, "1", TestBook.class);
        assertThat(found).isPresent();
        assertThat(found.get().title).isEqualTo("Make It So");
        assertThat(found.get().author).isEqualTo("Jean-Luc Picard");
        assertThat(found.get().year).isEqualTo(2350);
      });
    }

    @Test
    void returnsEmptyOptionalForNonExistentId() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);

        var found = template.findById(COLLECTION, "does-not-exist", TestBook.class);

        assertThat(found).isEmpty();
      });
    }

    @Test
    void savesMultipleDocumentsAndAllAreQueryable() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        var books = List.of(
            book("10", "The Hollow Men", "T.S. Eliot", 1925),
            book("11", "Heart of Darkness", "Joseph Conrad", 1899),
            book("12", "The Great Gatsby", "F. Scott Fitzgerald", 1925)
        );

        template.saveAll(COLLECTION, books);

        var query = new SimpleQuery(Criteria.where("id").in("10", "11", "12"));
        var results = template.queryForPage(COLLECTION, query, TestBook.class);
        assertThat(results.getTotalElements()).isEqualTo(3);
        assertThat(results.getContent()).extracting(b -> b.id)
            .containsExactlyInAnyOrder("10", "11", "12");
      });
    }
  }

  @Nested
  class QueryOperations {

    @Test
    void queriesDocumentsByFieldValueUsingCriteria() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        template.saveAll(COLLECTION, List.of(
            book("20", "Dune", "Frank Herbert", 1965),
            book("21", "Foundation", "Isaac Asimov", 1951),
            book("22", "Dune Messiah", "Frank Herbert", 1969)
        ));

        var query = new SimpleQuery(Criteria.where("author_s").is("Frank Herbert"));
        var results = template.queryForPage(COLLECTION, query, TestBook.class);

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).extracting(b -> b.title)
            .containsExactlyInAnyOrder("Dune", "Dune Messiah");
      });
    }

    @Test
    void countReturnsNumberOfMatchingDocuments() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        template.saveAll(COLLECTION, List.of(
            book("30", "1984", "George Orwell", 1949),
            book("31", "Animal Farm", "George Orwell", 1945),
            book("32", "Brave New World", "Aldous Huxley", 1932)
        ));

        var count = template.count(COLLECTION, new SimpleQuery(Criteria.where("author_s").is("George Orwell")));

        assertThat(count).isEqualTo(2);
      });
    }

    @Test
    void queryForPageReturnsPaginatedResultsWithCorrectTotalCount() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        template.saveAll(COLLECTION, List.of(
            book("40", "Book A", "Author X", 2000),
            book("41", "Book B", "Author X", 2001),
            book("42", "Book C", "Author X", 2002),
            book("43", "Book D", "Author X", 2003),
            book("44", "Book E", "Author X", 2004)
        ));

        var pageable = PageRequest.of(0, 2);
        var query = new SimpleQuery(Criteria.where("author_s").is("Author X"), pageable);
        var page = template.queryForPage(COLLECTION, query, TestBook.class);

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(3);
      });
    }

    @Test
    void criteriaRangeQueryFiltersDocumentsByYear() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        template.saveAll(COLLECTION, List.of(
            book("50", "Old Book", "Author Y", 1800),
            book("51", "Modern Book", "Author Y", 2000),
            book("52", "Future Book", "Author Y", 2100)
        ));

        var query = new SimpleQuery(Criteria.where("year_i").between(1900, 2099));
        var results = template.queryForPage(COLLECTION, query, TestBook.class);

        assertThat(results.getContent()).extracting(b -> b.title)
            .containsExactlyInAnyOrder("Modern Book");
      });
    }
  }

  @Nested
  class DeleteOperations {

    @Test
    void deleteByIdRemovesOnlyTheTargetDocument() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        template.saveAll(COLLECTION, List.of(
            book("60", "Stay", "Author Z", 2020),
            book("61", "Go", "Author Z", 2021)
        ));

        template.deleteById(COLLECTION, "61");
        template.commit(COLLECTION);

        assertThat(template.findById(COLLECTION, "61", TestBook.class)).isEmpty();
        assertThat(template.findById(COLLECTION, "60", TestBook.class)).isPresent();
      });
    }

    @Test
    void deleteByQueryRemovesAllMatchingDocuments() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        template.saveAll(COLLECTION, List.of(
            book("70", "Kept", "Author Keep", 2020),
            book("71", "Purged One", "Author Delete", 2021),
            book("72", "Purged Two", "Author Delete", 2022)
        ));

        template.deleteByQuery(COLLECTION, "author_s:\"Author Delete\"");
        template.commit(COLLECTION);

        assertThat(template.count(COLLECTION, new SimpleQuery(Criteria.where("author_s").is("Author Delete")))).isZero();
        assertThat(template.findById(COLLECTION, "70", TestBook.class)).isPresent();
      });
    }
  }

  @Nested
  class HealthIndicator {

    @Test
    void reportsUpWithCollectionDetailWhenSolrIsReachable() {
      contextRunner.run(ctx -> {
        var solrClient = ctx.getBean(SolrTemplate.class).getSolrClient();
        var indicator = new SolrHealthIndicator(solrClient, COLLECTION);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("collection", COLLECTION);
        assertThat(health.getDetails()).containsKey("elapsed_time_ms");
      });
    }

    @Test
    void reportsUpWithSolrVersionWhenNoCollectionConfigured() {
      contextRunner.run(ctx -> {
        var solrClient = ctx.getBean(SolrTemplate.class).getSolrClient();
        var indicator = new SolrHealthIndicator(solrClient, null);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("solr-version");
      });
    }
  }
}
