package dev.solrlazarus.autoconfigure.mapping;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolrDocumentResolverTest {

  @SolrDocument(collection = "books")
  static class BookWithExplicitCollection {}

  @SolrDocument
  static class ProductWithNoCollection {}

  static class NotAnnotated {}

  @Nested
  class ResolveCollection {

    @Test
    void resolvesCollectionFromAnnotation() {
      var collection = SolrDocumentResolver.resolveCollection(BookWithExplicitCollection.class);

      assertThat(collection).isEqualTo("books");
    }

    @Test
    void fallsBackToLowercaseClassNameWhenCollectionIsEmpty() {
      var collection = SolrDocumentResolver.resolveCollection(ProductWithNoCollection.class);

      assertThat(collection).isEqualTo("productwithnocollection");
    }

    @Test
    void throwsIllegalArgumentExceptionForNonAnnotatedClass() {
      assertThatThrownBy(() -> SolrDocumentResolver.resolveCollection(NotAnnotated.class))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("NotAnnotated");
    }
  }

  @Nested
  class IsSolrDocument {

    @Test
    void returnsTrueForAnnotatedClass() {
      assertThat(SolrDocumentResolver.isSolrDocument(BookWithExplicitCollection.class)).isTrue();
    }

    @Test
    void returnsTrueForAnnotatedClassWithNoExplicitCollection() {
      assertThat(SolrDocumentResolver.isSolrDocument(ProductWithNoCollection.class)).isTrue();
    }

    @Test
    void returnsFalseForNonAnnotatedClass() {
      assertThat(SolrDocumentResolver.isSolrDocument(NotAnnotated.class)).isFalse();
    }
  }
}
