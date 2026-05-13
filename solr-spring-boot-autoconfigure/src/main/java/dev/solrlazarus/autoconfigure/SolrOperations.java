package dev.solrlazarus.autoconfigure;

import dev.solrlazarus.autoconfigure.query.SimpleQuery;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.springframework.data.domain.Pageable;

public interface SolrOperations {

  <T> T save(String collection, T entity);

  <T> List<T> saveAll(String collection, Collection<T> entities);

  void savePartialUpdate(String collection, PartialUpdate update);

  <T> Optional<T> findById(String collection, String id, Class<T> type);

  <T> List<T> query(String collection, SolrQuery query, Class<T> type);

  <T> SolrPage<T> queryForPage(String collection, SimpleQuery query, Class<T> type);

  long count(String collection, SimpleQuery query);

  long count(String collection, SolrQuery query);

  void deleteById(String collection, String id);

  void deleteByQuery(String collection, String query);

  void commit(String collection);

  void softCommit(String collection);

  // @SolrDocument-aware convenience methods

  <T> T save(T entity);

  <T> Optional<T> findById(String id, Class<T> type);

  <T> SolrPage<T> queryForPage(SimpleQuery query, Class<T> type, Pageable pageable);

  SolrClient getSolrClient();
}
