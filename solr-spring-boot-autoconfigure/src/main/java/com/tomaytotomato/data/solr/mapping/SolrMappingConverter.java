package com.tomaytotomato.data.solr.mapping;

/**
 * Central holder for {@link SolrCustomConversions} used by {@link SolrDocumentReader} and
 * {@link SolrDocumentWriter} during document mapping.
 */
public class SolrMappingConverter {

  private final SolrCustomConversions conversions;

  /**
   * Creates a {@code SolrMappingConverter} backed by the given custom conversions.
   *
   * @param conversions the custom conversions to use; must not be {@code null}
   */
  public SolrMappingConverter(SolrCustomConversions conversions) {
    this.conversions = conversions;
  }

  /**
   * Creates a {@code SolrMappingConverter} with no custom converters registered.
   */
  public SolrMappingConverter() {
    this(SolrCustomConversions.empty());
  }

  /**
   * Returns the custom conversions registered with this converter.
   *
   * @return the custom conversions; never {@code null}
   */
  public SolrCustomConversions getConversions() {
    return conversions;
  }
}
