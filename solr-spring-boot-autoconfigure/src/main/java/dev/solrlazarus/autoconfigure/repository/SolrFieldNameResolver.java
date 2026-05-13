package dev.solrlazarus.autoconfigure.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.apache.solr.client.solrj.beans.Field;

public class SolrFieldNameResolver {

  private static final Map<Class<?>, SolrFieldNameResolver> CACHE = new ConcurrentHashMap<>();

  private final Map<String, String> propertyToSolrField;

  private SolrFieldNameResolver(Map<String, String> propertyToSolrField) {
    this.propertyToSolrField = propertyToSolrField;
  }

  public static SolrFieldNameResolver forClass(Class<?> entityClass) {
    return CACHE.computeIfAbsent(entityClass, SolrFieldNameResolver::buildMapping);
  }

  public static SolrFieldNameResolver identity() {
    return new SolrFieldNameResolver(Map.of());
  }

  public String resolve(String propertyName) {
    return propertyToSolrField.getOrDefault(propertyName, propertyName);
  }

  public UnaryOperator<String> asFunction() {
    return this::resolve;
  }

  private static SolrFieldNameResolver buildMapping(Class<?> clazz) {
    var mapping = new HashMap<String, String>();

    for (var field : clazz.getDeclaredFields()) {
      var annotation = field.getAnnotation(Field.class);
      if (annotation != null && !annotation.value().isEmpty() && !"#default".equals(annotation.value())) {
        mapping.put(field.getName(), annotation.value());
      }
    }

    for (var method : clazz.getDeclaredMethods()) {
      var annotation = method.getAnnotation(Field.class);
      if (annotation != null && !annotation.value().isEmpty() && !"#default".equals(annotation.value())) {
        var name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
          var propertyName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
          mapping.put(propertyName, annotation.value());
        } else if (name.startsWith("is") && name.length() > 2) {
          var propertyName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
          mapping.put(propertyName, annotation.value());
        }
      }
    }

    return new SolrFieldNameResolver(Map.copyOf(mapping));
  }
}
