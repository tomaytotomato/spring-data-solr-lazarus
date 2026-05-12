package dev.solrlazarus.autoconfigure.mapping;

public class SolrDocumentResolver {

  private SolrDocumentResolver() {}

  public static String resolveCollection(Class<?> type) {
    var annotation = type.getAnnotation(SolrDocument.class);
    if (annotation == null) {
      throw new IllegalArgumentException(
          "Class '%s' is not annotated with @SolrDocument".formatted(type.getSimpleName()));
    }
    var collection = annotation.collection();
    return collection.isEmpty() ? type.getSimpleName().toLowerCase() : collection;
  }

  public static boolean isSolrDocument(Class<?> type) {
    return type.isAnnotationPresent(SolrDocument.class);
  }
}
