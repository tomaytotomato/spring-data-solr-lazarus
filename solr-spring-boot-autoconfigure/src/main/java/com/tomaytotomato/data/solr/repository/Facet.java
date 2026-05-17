package com.tomaytotomato.data.solr.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares faceting on a repository method. When present, the method must return
 * {@link com.tomaytotomato.data.solr.FacetPage} and the query is dispatched to
 * {@link com.tomaytotomato.data.solr.SolrTemplate#queryForFacetPage}.
 *
 * <pre>{@code
 * @Facet(fields = {"author", "genre"}, minCount = 1)
 * FacetPage<Book> findByTitleContaining(String term, Pageable pageable);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Facet {

  /** Fields to facet on. */
  String[] fields() default {};

  /** Facet queries (e.g. {@code "year:[2000 TO 2010]"}). */
  String[] queries() default {};

  /** Minimum document count for a facet value to be included. */
  int minCount() default 1;

  /** Maximum number of facet values returned per field. */
  int limit() default 100;
}
