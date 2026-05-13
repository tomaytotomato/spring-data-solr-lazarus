package com.tomaytotomato.data.solr.mapping;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolrCustomConversionsTest {

  @Nested
  class EmptyConversions {

    @Test
    void emptyConversionsHasNoConverters() {
      var conversions = SolrCustomConversions.empty();

      assertThat(conversions.getConverters()).isEmpty();
    }
  }

  @Nested
  class ConverterStorage {

    @Test
    void convertersAreStoredAndRetrievable() {
      SolrReadConverter<String, LocalDate> readConverter = LocalDate::parse;
      var conversions = new SolrCustomConversions(List.of(readConverter));

      assertThat(conversions.getConverters()).hasSize(1);
      assertThat(conversions.getConverters().get(0)).isSameAs(readConverter);
    }

    @Test
    void multipleConvertersAreAllStored() {
      SolrReadConverter<String, LocalDate> readConverter = LocalDate::parse;
      SolrWriteConverter<LocalDate, String> writeConverter = LocalDate::toString;
      var conversions = new SolrCustomConversions(List.of(readConverter, writeConverter));

      assertThat(conversions.getConverters()).hasSize(2);
    }
  }

  @Nested
  class Immutability {

    @Test
    void converterListIsImmutable() {
      var conversions = new SolrCustomConversions(List.of());

      assertThatThrownBy(() -> conversions.getConverters().add(new Object()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
