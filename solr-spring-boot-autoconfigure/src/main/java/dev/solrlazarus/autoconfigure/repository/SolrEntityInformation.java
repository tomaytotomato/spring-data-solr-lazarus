package dev.solrlazarus.autoconfigure.repository;

import dev.solrlazarus.autoconfigure.mapping.SolrDocumentResolver;
import org.springframework.data.repository.core.EntityInformation;

public class SolrEntityInformation<T, ID> implements EntityInformation<T, ID> {

  private final Class<T> entityClass;
  private final Class<ID> idClass;
  private final String collection;

  @SuppressWarnings("unchecked")
  public SolrEntityInformation(Class<T> entityClass, Class<ID> idClass) {
    this.entityClass = entityClass;
    this.idClass = idClass;
    this.collection = SolrDocumentResolver.resolveCollection(entityClass);
  }

  @Override
  public boolean isNew(T entity) {
    return false;
  }

  @Override
  public ID getId(T entity) {
    return null;
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
