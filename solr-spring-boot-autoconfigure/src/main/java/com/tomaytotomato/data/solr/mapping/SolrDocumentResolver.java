package com.tomaytotomato.data.solr.mapping;

import org.springframework.core.env.Environment;

public class SolrDocumentResolver {

  private SolrDocumentResolver() {}

  public static String resolveCollection(Class<?> type) {
    return resolveCollection(type, null);
  }

  public static String resolveCollection(Class<?> type, Environment environment) {
    var annotation = type.getAnnotation(SolrDocument.class);
    if (annotation == null) {
      throw new IllegalArgumentException(
          "Class '%s' is not annotated with @SolrDocument".formatted(type.getSimpleName()));
    }
    var collection = annotation.collection();
    if (collection.isEmpty()) {
      return type.getSimpleName().toLowerCase();
    }
    return environment != null ? environment.resolvePlaceholders(collection) : collection;
  }

  public static boolean isSolrDocument(Class<?> type) {
    return type.isAnnotationPresent(SolrDocument.class);
  }
}
