package dev.solrlazarus.autoconfigure.query;

import org.apache.solr.client.solrj.request.SolrQuery;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleQueryTest {

  @Nested
  class BasicConversion {

    @Test
    void setsQueryStringFromCriteria() {
      var query = new SimpleQuery(Criteria.where("title").is("Spring Boot"));
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getQuery()).isEqualTo("title:Spring\\ Boot");
    }

    @Test
    void defaultsToUnpaginatedQuery() {
      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getStart()).isNull();
      assertThat(solrQuery.getRows()).isNull();
    }
  }

  @Nested
  class Pagination {

    @Test
    void setsStartAndRowsFromPageable() {
      var pageable = PageRequest.of(2, 25);
      var query = new SimpleQuery(Criteria.where("*").is("*"), pageable);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getStart()).isEqualTo(50);
      assertThat(solrQuery.getRows()).isEqualTo(25);
    }

    @Test
    void setsStartAndRowsFromFirstPage() {
      var pageable = PageRequest.of(0, 10);
      var query = new SimpleQuery(Criteria.where("*").is("*"), pageable);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getStart()).isEqualTo(0);
      assertThat(solrQuery.getRows()).isEqualTo(10);
    }
  }

  @Nested
  class Sorting {

    @Test
    void mapsSingleAscendingSortOrder() {
      var sort = Sort.by(Sort.Direction.ASC, "title");
      var query = new SimpleQuery(Criteria.where("*").is("*")).setSort(sort);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getSortField()).isEqualTo("title asc");
    }

    @Test
    void mapsSingleDescendingSortOrder() {
      var sort = Sort.by(Sort.Direction.DESC, "price");
      var query = new SimpleQuery(Criteria.where("*").is("*")).setSort(sort);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getSortField()).isEqualTo("price desc");
    }

    @Test
    void mapsMultipleSortOrders() {
      var sort = Sort.by(Sort.Direction.ASC, "title").and(Sort.by(Sort.Direction.DESC, "price"));
      var query = new SimpleQuery(Criteria.where("*").is("*")).setSort(sort);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getSortField()).isEqualTo("title asc,price desc");
    }
  }

  @Nested
  class FilterQueries {

    @Test
    void addsSingleFilterQuery() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .addFilterQuery("inStock:true");
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getFilterQueries()).containsExactly("inStock:true");
    }

    @Test
    void addsMultipleFilterQueries() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .addFilterQuery("inStock:true")
          .addFilterQuery("category:books");
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getFilterQueries()).containsExactlyInAnyOrder("inStock:true", "category:books");
    }
  }

  @Nested
  class ProjectionFields {

    @Test
    void addsProjectionFieldsToFlParam() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .addProjectionOnField("id")
          .addProjectionOnField("title");
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getFields()).isEqualTo("id,title");
    }
  }

  @Nested
  class RequestHandler {

    @Test
    void setsQtParameter() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setRequestHandler("/select");
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getRequestHandler()).isEqualTo("/select");
    }
  }

  @Nested
  class DefType {

    @Test
    void setsDefTypeParameter() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setDefType("edismax");
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("defType")).isEqualTo("edismax");
    }
  }
}
