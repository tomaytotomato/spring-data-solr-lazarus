package com.tomaytotomato.data.solr.mapping;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SolrMappingConverterTest {

  @Nested
  class DefaultConstructor {

    @Test
    void defaultConstructorCreatesEmptyConversions() {
      var converter = new SolrMappingConverter();

      assertThat(converter.getConversions().getConverters()).isEmpty();
    }
  }

  @Nested
  class CustomConversions {

    @Test
    void customConversionsAreAccessible() {
      SolrReadConverter<String, LocalDate> readConverter = LocalDate::parse;
      var conversions = new SolrCustomConversions(List.of(readConverter));

      var converter = new SolrMappingConverter(conversions);

      assertThat(converter.getConversions()).isSameAs(conversions);
    }

    @Test
    void convertersFromCustomConversionsAreRetrievable() {
      SolrReadConverter<String, LocalDate> readConverter = LocalDate::parse;
      var conversions = new SolrCustomConversions(List.of(readConverter));

      var converter = new SolrMappingConverter(conversions);

      assertThat(converter.getConversions().getConverters()).hasSize(1);
    }
  }
}
