package com.tomaytotomato.data.solr.mapping;

import org.apache.solr.common.SolrInputDocument;

public class SolrDocumentWriter<T> implements SolrDocumentConverter<T, SolrInputDocument> {

  private final Class<T> type;
  private final SolrCustomConversions conversions;

  public SolrDocumentWriter(Class<T> type) {
    this(type, SolrCustomConversions.empty());
  }

  public SolrDocumentWriter(Class<T> type, SolrCustomConversions conversions) {
    this.type = type;
    this.conversions = conversions;
  }

  @Override
  public SolrInputDocument convert(T source) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
