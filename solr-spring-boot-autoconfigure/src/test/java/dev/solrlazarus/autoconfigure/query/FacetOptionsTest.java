package dev.solrlazarus.autoconfigure.query;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FacetOptionsTest {

  @Nested
  class Defaults {

    @Test
    void minCountDefaultsToOne() {
      var options = new FacetOptions();
      assertThat(options.getMinCount()).isEqualTo(1);
    }

    @Test
    void limitDefaultsToTen() {
      var options = new FacetOptions();
      assertThat(options.getLimit()).isEqualTo(10);
    }

    @Test
    void sortDefaultsToNull() {
      var options = new FacetOptions();
      assertThat(options.getSort()).isNull();
    }

    @Test
    void facetFieldsDefaultToEmpty() {
      var options = new FacetOptions();
      assertThat(options.getFacetFields()).isEmpty();
    }

    @Test
    void facetQueriesDefaultToEmpty() {
      var options = new FacetOptions();
      assertThat(options.getFacetQueries()).isEmpty();
    }
  }

  @Nested
  class FluentSetters {

    @Test
    void minCountCanBeChanged() {
      var options = new FacetOptions().minCount(5);
      assertThat(options.getMinCount()).isEqualTo(5);
    }

    @Test
    void limitCanBeChanged() {
      var options = new FacetOptions().limit(25);
      assertThat(options.getLimit()).isEqualTo(25);
    }

    @Test
    void sortCanBeSet() {
      var options = new FacetOptions().sort("count");
      assertThat(options.getSort()).isEqualTo("count");
    }

    @Test
    void settersReturnSameInstance() {
      var options = new FacetOptions();
      assertThat(options.minCount(2)).isSameAs(options);
      assertThat(options.limit(20)).isSameAs(options);
      assertThat(options.sort("index")).isSameAs(options);
    }
  }

  @Nested
  class AddingFacetFields {

    @Test
    void addFacetOnFieldAppendsThenReturnsItself() {
      var options = new FacetOptions();
      var returned = options.addFacetOnField("category");
      assertThat(returned).isSameAs(options);
      assertThat(options.getFacetFields()).containsExactly("category");
    }

    @Test
    void multipleFacetFieldsAreAccumulated() {
      var options = new FacetOptions()
          .addFacetOnField("category")
          .addFacetOnField("author");
      assertThat(options.getFacetFields()).containsExactly("category", "author");
    }
  }

  @Nested
  class AddingFacetQueries {

    @Test
    void addFacetQueryAppendsThenReturnsItself() {
      var options = new FacetOptions();
      var returned = options.addFacetQuery("price:[0 TO 10]");
      assertThat(returned).isSameAs(options);
      assertThat(options.getFacetQueries()).containsExactly("price:[0 TO 10]");
    }

    @Test
    void multipleFacetQueriesAreAccumulated() {
      var options = new FacetOptions()
          .addFacetQuery("price:[0 TO 10]")
          .addFacetQuery("price:[10 TO 100]");
      assertThat(options.getFacetQueries()).containsExactly("price:[0 TO 10]", "price:[10 TO 100]");
    }
  }
}
