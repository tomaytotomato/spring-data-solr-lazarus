package com.tomaytotomato.data.solr.mapping;

@FunctionalInterface
public interface SolrDocumentConverter<S, T> {

  T convert(S source);
}
