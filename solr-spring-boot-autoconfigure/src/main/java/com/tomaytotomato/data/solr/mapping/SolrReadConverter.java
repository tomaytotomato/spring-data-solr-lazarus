package com.tomaytotomato.data.solr.mapping;

/**
 * Strategy interface for converting a Solr field value of type {@code S} into a domain object of
 * type {@code T} during a read operation.
 *
 * <p>Custom converters are registered but not yet applied automatically during document mapping.
 * This will be implemented in a future version.
 *
 * @param <S> the source type (typically the raw Solr field value)
 * @param <T> the target domain type
 */
@FunctionalInterface
public interface SolrReadConverter<S, T> {

  T convert(S source);
}
