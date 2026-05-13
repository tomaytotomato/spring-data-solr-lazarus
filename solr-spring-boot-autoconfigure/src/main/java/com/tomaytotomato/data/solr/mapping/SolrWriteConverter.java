package com.tomaytotomato.data.solr.mapping;

/**
 * Strategy interface for converting a domain object of type {@code S} into a Solr-compatible
 * value of type {@code T} during a write operation.
 *
 * <p>Custom converters are registered but not yet applied automatically during document mapping.
 * This will be implemented in a future version.
 *
 * @param <S> the source domain type
 * @param <T> the target type (typically the Solr field value representation)
 */
@FunctionalInterface
public interface SolrWriteConverter<S, T> {

  T convert(S source);
}
