package dev.solrlazarus.autoconfigure;

import dev.solrlazarus.autoconfigure.mapping.SolrDocumentResolver;
import dev.solrlazarus.autoconfigure.query.SimpleQuery;
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
import org.springframework.data.domain.Pageable;

public class SolrTemplate implements SolrOperations {

  private final SolrClient solrClient;
  private final CommitMode commitMode;

  public SolrTemplate(SolrClient solrClient) {
    this(solrClient, CommitMode.NONE);
  }

  public SolrTemplate(SolrClient solrClient, CommitMode commitMode) {
    this.solrClient = solrClient;
    this.commitMode = commitMode;
  }

  @Override
  public <T> T save(String collection, T entity) {
    try {
      solrClient.addBean(collection, entity);
      commitIfImmediate(collection);
      return entity;
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to save entity to collection: " + collection, e);
    }
  }

  @Override
  public <T> T save(T entity) {
    var collection = SolrDocumentResolver.resolveCollection(entity.getClass());
    return save(collection, entity);
  }

  @Override
  public <T> List<T> saveAll(String collection, Collection<T> entities) {
    try {
      solrClient.addBeans(collection, entities);
      commitIfImmediate(collection);
      return new ArrayList<>(entities);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to save entities to collection: " + collection, e);
    }
  }

  @Override
  public void savePartialUpdate(String collection, PartialUpdate update) {
    try {
      solrClient.add(collection, update.toSolrInputDocument());
      commitIfImmediate(collection);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to save partial update to collection: " + collection, e);
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
  public <T> Optional<T> findById(String id, Class<T> type) {
    var collection = SolrDocumentResolver.resolveCollection(type);
    return findById(collection, id, type);
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
  public <T> SolrPage<T> queryForPage(String collection, SimpleQuery query, Class<T> type) {
    try {
      var solrQuery = query.toSolrQuery();
      QueryResponse response = solrClient.query(collection, solrQuery);
      var beans = response.getBeans(type);
      long numFound = response.getResults().getNumFound();
      Float maxScore = response.getResults().getMaxScore();

      int start = solrQuery.getStart() != null ? solrQuery.getStart() : 0;
      int rows = solrQuery.getRows() != null ? solrQuery.getRows() : 10;
      var pageable = Pageable.ofSize(rows).withPage(rows > 0 ? start / rows : 0);

      return SolrPage.of(beans, pageable, numFound, maxScore);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to query collection: " + collection, e);
    }
  }

  @Override
  public <T> SolrPage<T> queryForPage(SimpleQuery query, Class<T> type, Pageable pageable) {
    var collection = SolrDocumentResolver.resolveCollection(type);
    return queryForPage(collection, query, type);
  }

  @Override
  public long count(String collection, SimpleQuery query) {
    try {
      var solrQuery = query.toSolrQuery();
      solrQuery.setRows(0);
      QueryResponse response = solrClient.query(collection, solrQuery);
      return response.getResults().getNumFound();
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to count in collection: " + collection, e);
    }
  }

  @Override
  public void deleteById(String collection, String id) {
    try {
      solrClient.deleteById(collection, id);
      commitIfImmediate(collection);
    } catch (IOException | SolrServerException e) {
      throw new SolrException(
          "Failed to delete entity by id '%s' from collection: %s".formatted(id, collection), e);
    }
  }

  @Override
  public void deleteByQuery(String collection, String query) {
    try {
      solrClient.deleteByQuery(collection, query);
      commitIfImmediate(collection);
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
  public void softCommit(String collection) {
    try {
      solrClient.commit(collection, true, true, true);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to soft commit collection: " + collection, e);
    }
  }

  @Override
  public SolrClient getSolrClient() {
    return solrClient;
  }

  private void commitIfImmediate(String collection) {
    if (commitMode == CommitMode.IMMEDIATE) {
      commit(collection);
    }
  }
}
