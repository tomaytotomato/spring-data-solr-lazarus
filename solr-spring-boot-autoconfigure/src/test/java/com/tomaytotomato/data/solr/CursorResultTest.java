package com.tomaytotomato.data.solr;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CursorResultTest {

  @Nested
  class Of {

    @Test
    void hasMoreIsTrueWhenNextCursorMarkDiffers() {
      var result = CursorResult.of(List.of("a", "b"), "AoE=", "BqF=");
      assertThat(result.hasMore()).isTrue();
      assertThat(result.cursorMark()).isEqualTo("BqF=");
      assertThat(result.content()).containsExactly("a", "b");
    }

    @Test
    void hasMoreIsFalseWhenNextCursorMarkEqualsRequest() {
      var result = CursorResult.of(List.of("a"), "AoE=", "AoE=");
      assertThat(result.hasMore()).isFalse();
      assertThat(result.cursorMark()).isEqualTo("AoE=");
    }

    @Test
    void hasMoreIsFalseWhenNextCursorMarkIsNull() {
      var result = CursorResult.of(List.of("x"), "AoE=", null);
      assertThat(result.hasMore()).isFalse();
      assertThat(result.cursorMark()).isNull();
    }

    @Test
    void contentIsAccessible() {
      var items = List.of("item1", "item2", "item3");
      var result = CursorResult.of(items, "*", "AoE=");
      assertThat(result.content()).isEqualTo(items);
    }
  }

  @Nested
  class Empty {

    @Test
    void returnsEmptyContentWithNullCursorMarkAndHasMoreFalse() {
      var result = CursorResult.empty();
      assertThat(result.content()).isEmpty();
      assertThat(result.cursorMark()).isNull();
      assertThat(result.hasMore()).isFalse();
    }
  }
}
