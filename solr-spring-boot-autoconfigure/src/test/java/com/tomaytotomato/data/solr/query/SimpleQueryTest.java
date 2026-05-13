package com.tomaytotomato.data.solr.query;

import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.params.FacetParams;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

  @Nested
  class Highlighting {

    @Test
    void enablesHighlightingWhenOptionsAreSet() {
      var options = new HighlightOptions().addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getHighlight()).isTrue();
    }

    @Test
    void doesNotEnableHighlightingWhenNoOptionsSet() {
      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var solrQuery = query.toSolrQuery();
      assertThatCode(() -> solrQuery.getHighlight()).doesNotThrowAnyException();
      assertThat(solrQuery.getBool("hl")).isNull();
    }

    @Test
    void setsHighlightFields() {
      var options = new HighlightOptions().addField("title").addField("description");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getHighlightFields()).containsExactly("title", "description");
    }

    @Test
    void setsCustomPreTag() {
      var options = new HighlightOptions().preTag("<b>").addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("hl.tag.pre")).isEqualTo("<b>");
    }

    @Test
    void setsCustomPostTag() {
      var options = new HighlightOptions().postTag("</b>").addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("hl.tag.post")).isEqualTo("</b>");
    }

    @Test
    void setsSnippets() {
      var options = new HighlightOptions().snippets(3).addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("hl.snippets")).isEqualTo("3");
    }

    @Test
    void setsFragsize() {
      var options = new HighlightOptions().fragsize(200).addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("hl.fragsize")).isEqualTo("200");
    }

    @Test
    void highlightOptionsReturnedFromGetter() {
      var options = new HighlightOptions().addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setHighlightOptions(options);
      assertThat(query.getHighlightOptions()).isSameAs(options);
    }

    @Test
    void highlightOptionsDefaultsToNull() {
      var query = new SimpleQuery(Criteria.where("*").is("*"));
      assertThat(query.getHighlightOptions()).isNull();
    }
  }

  @Nested
  class Faceting {

    @Test
    void enablesFacetWhenFacetOptionsPresent() {
      var facetOptions = new FacetOptions().addFacetOnField("category");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getBool(FacetParams.FACET)).isTrue();
    }

    @Test
    void setsFacetFieldParam() {
      var facetOptions = new FacetOptions()
          .addFacetOnField("category")
          .addFacetOnField("author");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getFacetFields()).containsExactly("category", "author");
    }

    @Test
    void setsFacetMinCount() {
      var facetOptions = new FacetOptions().addFacetOnField("genre").minCount(3);
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get(FacetParams.FACET_MINCOUNT)).isEqualTo("3");
    }

    @Test
    void setsFacetLimit() {
      var facetOptions = new FacetOptions().addFacetOnField("genre").limit(50);
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get(FacetParams.FACET_LIMIT)).isEqualTo("50");
    }

    @Test
    void setsFacetSortWhenPresent() {
      var facetOptions = new FacetOptions().addFacetOnField("genre").sort("count");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get(FacetParams.FACET_SORT)).isEqualTo("count");
    }

    @Test
    void doesNotSetFacetSortWhenNull() {
      var facetOptions = new FacetOptions().addFacetOnField("genre");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get(FacetParams.FACET_SORT)).isNull();
    }

    @Test
    void setsFacetQueryParams() {
      var facetOptions = new FacetOptions()
          .addFacetQuery("price:[0 TO 10]")
          .addFacetQuery("price:[10 TO 100]");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getParams(FacetParams.FACET_QUERY))
          .containsExactly("price:[0 TO 10]", "price:[10 TO 100]");
    }

    @Test
    void doesNotEnableFacetWhenNoFacetOptionsSet() {
      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.getBool(FacetParams.FACET)).isNull();
    }

    @Test
    void getFacetOptionsReturnsWhatWasSet() {
      var facetOptions = new FacetOptions().addFacetOnField("category");
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setFacetOptions(facetOptions);
      assertThat(query.getFacetOptions()).isSameAs(facetOptions);
    }
  }

  @Nested
  class CursorMark {

    @Test
    void setsCursorMarkParamOnSolrQueryWhenPresent() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setCursorMark("*");
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("cursorMark")).isEqualTo("*");
    }

    @Test
    void doesNotSetCursorMarkParamWhenNull() {
      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var solrQuery = query.toSolrQuery();
      assertThat(solrQuery.get("cursorMark")).isNull();
    }

    @Test
    void returnsCursorMarkValueFromGetter() {
      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setCursorMark("AoE=");
      assertThat(query.getCursorMark()).isEqualTo("AoE=");
    }
  }
}
