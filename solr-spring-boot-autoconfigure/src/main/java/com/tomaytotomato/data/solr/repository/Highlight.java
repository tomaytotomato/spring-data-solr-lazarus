package com.tomaytotomato.data.solr.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares highlighting on a repository method. When present, the method must return
 * {@link com.tomaytotomato.data.solr.HighlightPage} and the query is dispatched to
 * {@link com.tomaytotomato.data.solr.SolrTemplate#queryForHighlightPage}.
 *
 * <pre>{@code
 * @Highlight(fields = "title", prefix = "<mark>", postfix = "</mark>")
 * HighlightPage<Book> findByTitleContaining(String term, Pageable pageable);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Highlight {

  /** Solr fields to highlight. Empty means Solr highlights all fields. */
  String[] fields() default {};

  /** Number of characters per highlight fragment. */
  int fragsize() default 100;

  /** Maximum number of highlight snippets per field per document. */
  int snippets() default 1;

  /** Opening tag inserted before each matched term. */
  String prefix() default "<em>";

  /** Closing tag inserted after each matched term. */
  String postfix() default "</em>";
}
