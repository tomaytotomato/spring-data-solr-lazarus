package com.tomaytotomato.data.solr.mapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.solr.common.SolrInputDocument;

public class SolrDocumentWriter<T> implements SolrDocumentConverter<T, SolrInputDocument> {

  @Override
  public SolrInputDocument convert(T source) {
    try {
      var doc = new SolrInputDocument();
      for (var field : annotatedFields(source.getClass())) {
        var solrFieldName = solrFieldName(field);
        field.setAccessible(true);
        var value = field.get(source);
        if (value == null) {
          continue;
        }
        if (value instanceof Collection<?> collection) {
          doc.setField(solrFieldName, new ArrayList<>(collection));
        } else {
          doc.setField(solrFieldName, value);
        }
      }
      return doc;
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          "Failed to convert entity to SolrInputDocument: " + source.getClass().getName(), e);
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
