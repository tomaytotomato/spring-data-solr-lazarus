package dev.solrlazarus.autoconfigure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.SolrQuery;

public interface SolrOperations {

  <T> T save(String collection, T entity);

  <T> List<T> saveAll(String collection, Collection<T> entities);

  <T> Optional<T> findById(String collection, String id, Class<T> type);

  <T> List<T> query(String collection, SolrQuery query, Class<T> type);

  void deleteById(String collection, String id);

  void deleteByQuery(String collection, String query);

  void commit(String collection);

  SolrClient getSolrClient();
}
