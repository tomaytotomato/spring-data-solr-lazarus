package dev.solrlazarus.autoconfigure;

import dev.solrlazarus.autoconfigure.query.FacetFieldEntry;
import dev.solrlazarus.autoconfigure.query.FacetQueryEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;

class FacetPageTest {

  @Nested
  class WithFieldFacets {

    @Test
    void holdsFieldFacetEntries() {
      var entries = List.of(new FacetFieldEntry("sci-fi", 42L), new FacetFieldEntry("drama", 17L));
      var page = new FacetPage<>(List.of("doc1"), Pageable.ofSize(10), 1L,
          Map.of("genre", entries), List.of());

      assertThat(page.getFacetFields()).containsKey("genre");
      assertThat(page.getFacetField("genre")).containsExactlyElementsOf(entries);
    }

    @Test
    void getFacetFieldReturnsCorrectEntriesForNamedField() {
      var genreEntries = List.of(new FacetFieldEntry("sci-fi", 10L));
      var authorEntries = List.of(new FacetFieldEntry("Asimov", 5L));
      var page = new FacetPage<>(List.of(), Pageable.ofSize(10), 0L,
          Map.of("genre", genreEntries, "author", authorEntries), List.of());

      assertThat(page.getFacetField("genre")).isEqualTo(genreEntries);
      assertThat(page.getFacetField("author")).isEqualTo(authorEntries);
    }

    @Test
    void getFacetFieldReturnsEmptyListForUnknownField() {
      var page = new FacetPage<>(List.of(), Pageable.ofSize(10), 0L, Map.of(), List.of());

      assertThat(page.getFacetField("nonexistent")).isEmpty();
    }
  }

  @Nested
  class WithQueryFacets {

    @Test
    void holdsFacetQueryEntries() {
      var entries = List.of(
          new FacetQueryEntry("price:[0 TO 10]", 23L),
          new FacetQueryEntry("price:[10 TO 100]", 87L));
      var page = new FacetPage<>(List.of(), Pageable.ofSize(10), 0L, Map.of(), entries);

      assertThat(page.getFacetQueries()).containsExactlyElementsOf(entries);
    }
  }

  @Nested
  class PageBehaviour {

    @Test
    void extendsPageImplSoTotalElementsIsAccessible() {
      var page = new FacetPage<>(List.of("a", "b"), Pageable.ofSize(10), 99L, Map.of(), List.of());

      assertThat(page.getTotalElements()).isEqualTo(99L);
    }

    @Test
    void contentIsAccessible() {
      var page = new FacetPage<>(List.of("x"), Pageable.ofSize(10), 1L, Map.of(), List.of());

      assertThat(page.getContent()).containsExactly("x");
    }
  }
}
