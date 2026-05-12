package dev.solrlazarus.autoconfigure.repository;

import dev.solrlazarus.autoconfigure.SolrTemplate;
import dev.solrlazarus.autoconfigure.mapping.SolrDocumentResolver;
import dev.solrlazarus.autoconfigure.query.Criteria;
import dev.solrlazarus.autoconfigure.query.SimpleQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SimpleSolrRepository<T, ID> implements SolrRepository<T, ID> {

  private final SolrTemplate solrTemplate;
  private final Class<T> entityClass;
  private final String collection;

  public SimpleSolrRepository(SolrTemplate solrTemplate, Class<T> entityClass) {
    this.solrTemplate = solrTemplate;
    this.entityClass = entityClass;
    this.collection = SolrDocumentResolver.resolveCollection(entityClass);
  }

  @Override
  public <S extends T> S save(S entity) {
    return solrTemplate.save(collection, entity);
  }

  @Override
  public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
    var list = new ArrayList<S>();
    entities.forEach(list::add);
    return solrTemplate.saveAll(collection, list);
  }

  @Override
  public Optional<T> findById(ID id) {
    return solrTemplate.findById(collection, id.toString(), entityClass);
  }

  @Override
  public boolean existsById(ID id) {
    return findById(id).isPresent();
  }

  @Override
  public Iterable<T> findAll() {
    return solrTemplate.query(collection, new SolrQuery("*:*"), entityClass);
  }

  @Override
  public Iterable<T> findAllById(Iterable<ID> ids) {
    var idList = StreamSupport.stream(ids.spliterator(), false)
        .map(Object::toString)
        .collect(Collectors.toList());
    if (idList.isEmpty()) {
      return List.of();
    }
    var idQuery = "id:(" + String.join(" OR ", idList) + ")";
    return solrTemplate.query(collection, new SolrQuery(idQuery), entityClass);
  }

  @Override
  public long count() {
    return solrTemplate.count(collection, new SimpleQuery(Criteria.where("*").expression("*")));
  }

  @Override
  public void deleteById(ID id) {
    solrTemplate.deleteById(collection, id.toString());
  }

  @Override
  public void delete(T entity) {
    throw new UnsupportedOperationException(
        "delete(entity) requires @Id field reflection — use deleteById instead");
  }

  @Override
  public void deleteAllById(Iterable<? extends ID> ids) {
    ids.forEach(this::deleteById);
  }

  @Override
  public void deleteAll(Iterable<? extends T> entities) {
    throw new UnsupportedOperationException(
        "deleteAll(entities) requires @Id field reflection — use deleteAllById instead");
  }

  @Override
  public void deleteAll() {
    solrTemplate.deleteByQuery(collection, "*:*");
    solrTemplate.commit(collection);
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    var query = new SimpleQuery(Criteria.where("*").expression("*"), pageable);
    return solrTemplate.queryForPage(collection, query, entityClass);
  }

  @Override
  public Iterable<T> findAll(Sort sort) {
    var query = new SimpleQuery(Criteria.where("*").expression("*"));
    query.setSort(sort);
    return solrTemplate.query(collection, query.toSolrQuery(), entityClass);
  }
}
