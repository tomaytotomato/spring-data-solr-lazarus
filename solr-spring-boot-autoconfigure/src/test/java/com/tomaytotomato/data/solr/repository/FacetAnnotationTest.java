package com.tomaytotomato.data.solr.repository;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FacetAnnotationTest {

  @Nested
  class AnnotationDefaults {

    @Facet
    void annotatedWithDefaults() {}

    @Test
    void defaultFieldsIsEmpty() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Facet.class);

      assertThat(annotation.fields()).isEmpty();
    }

    @Test
    void defaultQueriesIsEmpty() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Facet.class);

      assertThat(annotation.queries()).isEmpty();
    }

    @Test
    void defaultMinCountIs1() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Facet.class);

      assertThat(annotation.minCount()).isEqualTo(1);
    }

    @Test
    void defaultLimitIs100() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithDefaults")
          .getAnnotation(Facet.class);

      assertThat(annotation.limit()).isEqualTo(100);
    }
  }

  @Nested
  class CustomAttributes {

    @Facet(fields = {"author", "genre"}, queries = {"year:[2000 TO 2010]"}, minCount = 5, limit = 50)
    void annotatedWithCustomValues() {}

    @Test
    void customFieldsArePreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Facet.class);

      assertThat(annotation.fields()).containsExactly("author", "genre");
    }

    @Test
    void customQueriesArePreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Facet.class);

      assertThat(annotation.queries()).containsExactly("year:[2000 TO 2010]");
    }

    @Test
    void customMinCountIsPreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Facet.class);

      assertThat(annotation.minCount()).isEqualTo(5);
    }

    @Test
    void customLimitIsPreserved() throws Exception {
      var annotation = getClass().getDeclaredMethod("annotatedWithCustomValues")
          .getAnnotation(Facet.class);

      assertThat(annotation.limit()).isEqualTo(50);
    }
  }

  @Nested
  class OptionsConstruction {

    @Test
    void buildsFacetOptionsFromAnnotationDefaults() throws Exception {
      var method = AnnotationDefaults.class.getDeclaredMethod("annotatedWithDefaults");
      var annotation = method.getAnnotation(Facet.class);

      var options = FacetAnnotationAdapter.toFacetOptions(annotation);

      assertThat(options.getMinCount()).isEqualTo(1);
      assertThat(options.getLimit()).isEqualTo(100);
      assertThat(options.getFacetFields()).isEmpty();
      assertThat(options.getFacetQueries()).isEmpty();
    }

    @Test
    void buildsFacetOptionsFromCustomAnnotation() throws Exception {
      var method = CustomAttributes.class.getDeclaredMethod("annotatedWithCustomValues");
      var annotation = method.getAnnotation(Facet.class);

      var options = FacetAnnotationAdapter.toFacetOptions(annotation);

      assertThat(options.getMinCount()).isEqualTo(5);
      assertThat(options.getLimit()).isEqualTo(50);
      assertThat(options.getFacetFields()).containsExactly("author", "genre");
      assertThat(options.getFacetQueries()).containsExactly("year:[2000 TO 2010]");
    }
  }
}
