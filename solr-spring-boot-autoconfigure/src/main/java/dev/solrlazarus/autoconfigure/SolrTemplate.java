package dev.solrlazarus.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;

public class SolrTemplate implements SolrOperations {

  private final SolrClient solrClient;

  public SolrTemplate(SolrClient solrClient) {
    this.solrClient = solrClient;
  }

  @Override
  public <T> T save(String collection, T entity) {
    try {
      solrClient.addBean(collection, entity);
      return entity;
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to save entity to collection: " + collection, e);
    }
  }

  @Override
  public <T> List<T> saveAll(String collection, Collection<T> entities) {
    try {
      solrClient.addBeans(collection, entities);
      return new ArrayList<>(entities);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to save entities to collection: " + collection, e);
    }
  }

  @Override
  public <T> Optional<T> findById(String collection, String id, Class<T> type) {
    try {
      var query = new SolrQuery("id:" + ClientUtils.escapeQueryChars(id));
      query.setRows(1);
      var response = solrClient.query(collection, query);
      var beans = response.getBeans(type);
      return beans.isEmpty() ? Optional.empty() : Optional.of(beans.getFirst());
    } catch (IOException | SolrServerException e) {
      throw new SolrException(
          "Failed to find entity by id '%s' in collection: %s".formatted(id, collection), e);
    }
  }

  @Override
  public <T> List<T> query(String collection, SolrQuery query, Class<T> type) {
    try {
      QueryResponse response = solrClient.query(collection, query);
      return response.getBeans(type);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to query collection: " + collection, e);
    }
  }

  @Override
  public void deleteById(String collection, String id) {
    try {
      solrClient.deleteById(collection, id);
    } catch (IOException | SolrServerException e) {
      throw new SolrException(
          "Failed to delete entity by id '%s' from collection: %s".formatted(id, collection), e);
    }
  }

  @Override
  public void deleteByQuery(String collection, String query) {
    try {
      solrClient.deleteByQuery(collection, query);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to delete by query from collection: " + collection, e);
    }
  }

  @Override
  public void commit(String collection) {
    try {
      solrClient.commit(collection);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to commit collection: " + collection, e);
    }
  }

  @Override
  public SolrClient getSolrClient() {
    return solrClient;
  }
}
