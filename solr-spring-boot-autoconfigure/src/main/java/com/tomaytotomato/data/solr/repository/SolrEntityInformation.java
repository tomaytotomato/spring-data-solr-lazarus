package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.mapping.SolrDocumentResolver;
import org.springframework.data.repository.core.EntityInformation;

public class SolrEntityInformation<T, ID> implements EntityInformation<T, ID> {

  private final Class<T> entityClass;
  private final Class<ID> idClass;
  private final String collection;
  private final java.lang.reflect.Field idField;

  @SuppressWarnings("unchecked")
  public SolrEntityInformation(Class<T> entityClass, Class<ID> idClass) {
    this.entityClass = entityClass;
    this.idClass = idClass;
    this.collection = SolrDocumentResolver.resolveCollection(entityClass);
    this.idField = resolveIdField(entityClass);
  }

  private static java.lang.reflect.Field resolveIdField(Class<?> entityClass) {
    for (var field : entityClass.getDeclaredFields()) {
      var annotation = field.getAnnotation(org.apache.solr.client.solrj.beans.Field.class);
      if (annotation == null) {
        continue;
      }
      var annotationValue = annotation.value();
      boolean isDefaultValue = annotationValue.isEmpty() || "#default".equals(annotationValue);
      if (isDefaultValue && "id".equals(field.getName())) {
        field.setAccessible(true);
        return field;
      }
      if (!isDefaultValue && "id".equals(annotationValue)) {
        field.setAccessible(true);
        return field;
      }
    }
    return null;
  }

  @Override
  public boolean isNew(T entity) {
    return getId(entity) == null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ID getId(T entity) {
    if (idField == null) {
      return null;
    }
    try {
      return (ID) idField.get(entity);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

  @Override
  public Class<ID> getIdType() {
    return idClass;
  }

  @Override
  public Class<T> getJavaType() {
    return entityClass;
  }

  public String getCollection() {
    return collection;
  }
}
