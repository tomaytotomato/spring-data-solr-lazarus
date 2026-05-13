package dev.solrlazarus.autoconfigure;

import dev.solrlazarus.autoconfigure.query.HighlightEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class HighlightPageTest {

  @Nested
  class Creation {

    @Test
    void holdsContentAndPageable() {
      var content = List.of("alpha", "beta");
      var pageable = Pageable.ofSize(10);
      var entries = List.<HighlightEntry<String>>of();

      var page = new HighlightPage<>(content, pageable, 42L, entries);

      assertThat(page.getContent()).containsExactly("alpha", "beta");
    }

    @Test
    void totalElementsReflectsNumFoundNotContentSize() {
      var content = List.of("alpha", "beta");
      var pageable = PageRequest.of(0, 10);
      var entries = List.<HighlightEntry<String>>of();

      var page = new HighlightPage<>(content, pageable, 200L, entries);

      assertThat(page.getTotalElements()).isEqualTo(200L);
    }

    @Test
    void highlightedEntriesAreAccessible() {
      var entity = "doc1";
      var highlights = Map.of("title", List.of("a <em>match</em>"));
      var entry = new HighlightEntry<>(entity, highlights);
      var pageable = Pageable.ofSize(10);

      var page = new HighlightPage<>(List.of(entity), pageable, 1L, List.of(entry));

      assertThat(page.getHighlighted()).containsExactly(entry);
    }

    @Test
    void emptyHighlightsListIsAllowed() {
      var content = List.of("doc1", "doc2");
      var pageable = Pageable.ofSize(10);

      var page = new HighlightPage<>(content, pageable, 2L, List.of());

      assertThat(page.getHighlighted()).isEmpty();
    }
  }

  @Nested
  class PageMetadata {

    @Test
    void pageNumberIsCorrect() {
      var content = List.of("x");
      var pageable = PageRequest.of(2, 10);
      var entries = List.<HighlightEntry<String>>of();

      var page = new HighlightPage<>(content, pageable, 100L, entries);

      assertThat(page.getNumber()).isEqualTo(2);
    }

    @Test
    void pageSizeIsCorrect() {
      var content = List.of("x");
      var pageable = PageRequest.of(0, 15);
      var entries = List.<HighlightEntry<String>>of();

      var page = new HighlightPage<>(content, pageable, 15L, entries);

      assertThat(page.getSize()).isEqualTo(15);
    }
  }

  @Nested
  class HighlightEntries {

    @Test
    void entryHoldsEntityAndHighlightMap() {
      var entity = "myDocument";
      var highlights = Map.of("body", List.of("first snippet", "second snippet"));
      var entry = new HighlightEntry<>(entity, highlights);

      assertThat(entry.entity()).isEqualTo(entity);
      assertThat(entry.highlights()).isEqualTo(highlights);
    }

    @Test
    void entryWithEmptyHighlightsIsValid() {
      var entity = "myDocument";
      var entry = new HighlightEntry<>(entity, Map.of());

      assertThat(entry.highlights()).isEmpty();
    }
  }
}
