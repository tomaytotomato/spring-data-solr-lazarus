package com.tomaytotomato.data.solr;

import com.tomaytotomato.data.solr.mapping.SolrDocumentResolver;
import com.tomaytotomato.data.solr.query.FacetFieldEntry;
import com.tomaytotomato.data.solr.query.FacetQueryEntry;
import com.tomaytotomato.data.solr.query.HighlightEntry;
import com.tomaytotomato.data.solr.query.SimpleQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  public <T> HighlightPage<T> queryForHighlightPage(String collection, SimpleQuery query, Class<T> type) {
    try {
      var solrQuery = query.toSolrQuery();
      QueryResponse response = solrClient.query(collection, solrQuery);
      var beans = response.getBeans(type);
      var docs = response.getResults();
      long numFound = docs.getNumFound();
      var highlighting = response.getHighlighting();

      int start = solrQuery.getStart() != null ? solrQuery.getStart() : 0;
      int rows = solrQuery.getRows() != null ? solrQuery.getRows() : 10;
      var pageable = Pageable.ofSize(rows).withPage(rows > 0 ? start / rows : 0);

      var entries = new ArrayList<HighlightEntry<T>>();
      for (int i = 0; i < beans.size(); i++) {
        var docId = docs.get(i).getFieldValue("id");
        var docHighlights = highlighting != null && docId != null
            ? highlighting.getOrDefault(docId.toString(), Map.of())
            : Map.<String, List<String>>of();
        entries.add(new HighlightEntry<>(beans.get(i), docHighlights));
      }

      return new HighlightPage<>(beans, pageable, numFound, entries);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to query with highlights for collection: " + collection, e);
    }
  }

  @Override
  public <T> FacetPage<T> queryForFacetPage(String collection, SimpleQuery query, Class<T> type) {
    try {
      var solrQuery = query.toSolrQuery();
      QueryResponse response = solrClient.query(collection, solrQuery);
      var beans = response.getBeans(type);
      long numFound = response.getResults().getNumFound();

      int start = solrQuery.getStart() != null ? solrQuery.getStart() : 0;
      int rows = solrQuery.getRows() != null ? solrQuery.getRows() : 10;
      var pageable = Pageable.ofSize(rows).withPage(rows > 0 ? start / rows : 0);

      var facetFieldMap = new LinkedHashMap<String, List<FacetFieldEntry>>();
      var facetFieldsResponse = response.getFacetFields();
      if (facetFieldsResponse != null) {
        for (var facetField : facetFieldsResponse) {
          var entries = facetField.getValues().stream()
              .map(count -> new FacetFieldEntry(count.getName(), count.getCount()))
              .toList();
          facetFieldMap.put(facetField.getName(), entries);
        }
      }

      var facetQueryEntries = new ArrayList<FacetQueryEntry>();
      var facetQueryResponse = response.getFacetQuery();
      if (facetQueryResponse != null) {
        facetQueryResponse.forEach((q, c) ->
            facetQueryEntries.add(new FacetQueryEntry(q, c)));
      }

      return new FacetPage<>(beans, pageable, numFound, facetFieldMap, facetQueryEntries);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to query with facets for collection: " + collection, e);
    }
  }

  @Override
  public long count(String collection, SimpleQuery query) {
    return count(collection, query.toSolrQuery());
  }

  @Override
  public long count(String collection, SolrQuery query) {
    try {
      var countQuery = query.getCopy();
      countQuery.setRows(0);
      QueryResponse response = solrClient.query(collection, countQuery);
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
  public <T> CursorResult<T> queryWithCursor(String collection, SimpleQuery query, Class<T> type) {
    try {
      var solrQuery = query.toSolrQuery();
      QueryResponse response = solrClient.query(collection, solrQuery);
      var beans = response.getBeans(type);
      var nextCursorMark = response.getNextCursorMark();
      return CursorResult.of(beans, query.getCursorMark(), nextCursorMark);
    } catch (IOException | SolrServerException e) {
      throw new SolrException("Failed to query with cursor for collection: " + collection, e);
    }
  }

  public SolrClient getSolrClient() {
    return solrClient;
  }

  private void commitIfImmediate(String collection) {
    if (commitMode == CommitMode.IMMEDIATE) {
      commit(collection);
    }
  }
}
