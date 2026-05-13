package com.tomaytotomato.data.solr.mapping;

/**
 * Central converter that wraps SolrJ's {@code DocumentObjectBinder} and provides a hook for
 * user-registered {@link SolrCustomConversions}.
 *
 * <p>In this v0.1 implementation the converter serves as the registration point only. Custom
 * converters are stored in the associated {@link SolrCustomConversions} but are not yet applied
 * automatically during document mapping. Integration with the read/write path is planned for a
 * future version.
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
