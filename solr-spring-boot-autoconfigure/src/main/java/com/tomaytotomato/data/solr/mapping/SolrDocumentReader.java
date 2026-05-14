package com.tomaytotomato.data.solr.mapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.common.SolrDocument;

public class SolrDocumentReader<T> implements SolrDocumentConverter<SolrDocument, T> {

  private final Class<T> type;

  public SolrDocumentReader(Class<T> type) {
    this.type = type;
  }

  @Override
  public T convert(SolrDocument source) {
    try {
      var instance = type.getDeclaredConstructor().newInstance();
      for (var field : annotatedFields(type)) {
        var solrFieldName = solrFieldName(field);
        var value = source.getFieldValue(solrFieldName);
        if (value == null) {
          continue;
        }
        field.setAccessible(true);
        field.set(instance, coerce(value, field.getType()));
      }
      setScore(instance, source);
      return instance;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to map SolrDocument to " + type.getName(), e);
    }
  }

  private Object coerce(Object value, Class<?> targetType) {
    if (targetType.isAssignableFrom(value.getClass())) {
      return value;
    }
    if (value instanceof Collection<?> collection && !Collection.class.isAssignableFrom(targetType)) {
      return coerceCollection(collection, targetType);
    }
    return value;
  }

  private Object coerceCollection(Collection<?> collection, Class<?> targetType) {
    if (collection.size() == 1) {
      var unwrapped = collection.iterator().next();
      if (unwrapped != null && targetType.isAssignableFrom(unwrapped.getClass())) {
        return unwrapped;
      }
    }
    if (targetType == String.class) {
      return collection.stream()
          .map(Object::toString)
          .collect(Collectors.joining(","));
    }
    throw new IllegalArgumentException(
        "Cannot convert %s to %s".formatted(collection.getClass().getName(), targetType.getName()));
  }

  private void setScore(T instance, SolrDocument source) throws IllegalAccessException {
    var score = source.getFieldValue("score");
    if (score == null) {
      return;
    }
    for (var field : type.getDeclaredFields()) {
      if (field.isAnnotationPresent(Score.class)) {
        field.setAccessible(true);
        field.set(instance, ((Number) score).floatValue());
        return;
      }
    }
  }

  private static String solrFieldName(Field field) {
    var annotation = field.getAnnotation(org.apache.solr.client.solrj.beans.Field.class);
    var value = annotation.value();
    if (value.isEmpty() || "#default".equals(value)) {
      return field.getName();
    }
    return value;
  }

  private static List<Field> annotatedFields(Class<?> clazz) {
    var fields = new ArrayList<Field>();
    var current = clazz;
    while (current != null && current != Object.class) {
      for (var field : current.getDeclaredFields()) {
        if (field.isAnnotationPresent(org.apache.solr.client.solrj.beans.Field.class)) {
          fields.add(field);
        }
      }
      current = current.getSuperclass();
    }
    return fields;
  }
}
