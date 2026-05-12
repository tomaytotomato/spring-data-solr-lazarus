package dev.solrlazarus.autoconfigure;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class SolrPageTest {

  @Nested
  class Creation {

    @Test
    void holdsContentAndPageable() {
      var content = List.of("alpha", "beta");
      var pageable = Pageable.ofSize(10);

      var page = SolrPage.of(content, pageable, 42L, 0.95f);

      assertThat(page.getContent()).containsExactly("alpha", "beta");
    }

    @Test
    void totalElementsReflectsNumFoundNotContentSize() {
      var content = List.of("alpha", "beta");
      var pageable = PageRequest.of(0, 10);

      var page = SolrPage.of(content, pageable, 250L, null);

      assertThat(page.getTotalElements()).isEqualTo(250L);
    }

    @Test
    void maxScoreIsAccessible() {
      var content = List.of("doc1");
      var pageable = Pageable.ofSize(5);

      var page = SolrPage.of(content, pageable, 1L, 1.23f);

      assertThat(page.getMaxScore()).isEqualTo(1.23f);
    }

    @Test
    void maxScoreCanBeNull() {
      var content = List.of("doc1");
      var pageable = Pageable.ofSize(5);

      var page = SolrPage.of(content, pageable, 1L, null);

      assertThat(page.getMaxScore()).isNull();
    }
  }

  @Nested
  class PageMetadata {

    @Test
    void pageNumberIsCorrect() {
      var content = List.of("x");
      var pageable = PageRequest.of(2, 10);

      var page = SolrPage.of(content, pageable, 100L, null);

      assertThat(page.getNumber()).isEqualTo(2);
    }

    @Test
    void pageSizeIsCorrect() {
      var content = List.of("x");
      var pageable = PageRequest.of(0, 15);

      var page = SolrPage.of(content, pageable, 15L, null);

      assertThat(page.getSize()).isEqualTo(15);
    }

    @Test
    void totalPagesIsCalculatedFromNumFoundAndPageSize() {
      var content = List.of("a", "b", "c", "d", "e");
      var pageable = PageRequest.of(0, 5);

      var page = SolrPage.of(content, pageable, 23L, null);

      assertThat(page.getTotalPages()).isEqualTo(5);
    }
  }

  @Nested
  class EmptyPage {

    @Test
    void emptyPageHasNoContent() {
      var pageable = Pageable.ofSize(10);

      var page = SolrPage.<String>of(List.of(), pageable, 0L, null);

      assertThat(page.getContent()).isEmpty();
    }

    @Test
    void emptyPageHasZeroTotalElements() {
      var pageable = Pageable.ofSize(10);

      var page = SolrPage.<String>of(List.of(), pageable, 0L, null);

      assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void emptyPageHasZeroTotalPages() {
      var pageable = Pageable.ofSize(10);

      var page = SolrPage.<String>of(List.of(), pageable, 0L, null);

      assertThat(page.getTotalPages()).isZero();
    }
  }
}
