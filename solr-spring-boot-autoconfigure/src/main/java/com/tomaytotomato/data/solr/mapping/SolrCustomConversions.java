package com.tomaytotomato.data.solr.mapping;

import java.util.List;

/**
 * Registry for user-defined {@link SolrReadConverter} and {@link SolrWriteConverter} instances.
 *
 * <p>Declare a {@code SolrCustomConversions} bean in your application context to supply custom
 * converters:
 *
 * <pre>{@code
 * @Bean
 * public SolrCustomConversions solrCustomConversions() {
 *     return new SolrCustomConversions(List.of(
 *         (SolrReadConverter<String, LocalDate>) LocalDate::parse
 *     ));
 * }
 * }</pre>
 *
 * <p>Custom converters are registered and stored here but are not yet applied automatically during
 * document mapping. Integration with the read/write path is planned for a future version.
 */
public class SolrCustomConversions {

  private final List<Object> converters;

  /**
   * Creates a new {@code SolrCustomConversions} with the given converter instances.
   *
   * @param converters the converters to register; must not be {@code null}
   */
  public SolrCustomConversions(List<?> converters) {
    this.converters = List.copyOf(converters);
  }

  /**
   * Returns the registered converters as an unmodifiable list.
   *
   * @return the converters; never {@code null}
   */
  public List<Object> getConverters() {
    return converters;
  }

  /**
   * Returns an empty {@code SolrCustomConversions} with no registered converters. Used as the
   * auto-configured default.
   *
   * @return an empty instance; never {@code null}
   */
  public static SolrCustomConversions empty() {
    return new SolrCustomConversions(List.of());
  }
}
