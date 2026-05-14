package com.tomaytotomato.data.solr.mapping;

import java.util.List;

/**
 * Registry for user-defined {@link SolrDocumentConverter} instances used during document mapping.
 *
 * <p>Declare a {@code SolrCustomConversions} bean in your application context to supply custom
 * converters:
 *
 * <pre>{@code
 * @Bean
 * public SolrCustomConversions solrCustomConversions() {
 *     return new SolrCustomConversions(List.of(
 *         (SolrDocumentConverter<String, LocalDate>) LocalDate::parse
 *     ));
 * }
 * }</pre>
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
