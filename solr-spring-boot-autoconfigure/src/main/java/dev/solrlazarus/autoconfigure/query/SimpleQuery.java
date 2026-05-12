package dev.solrlazarus.autoconfigure.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Wraps a {@link Criteria} with pagination, sorting, filter queries, and field projections,
 * converting the whole into a SolrJ {@link SolrQuery} via {@link #toSolrQuery()}.
 */
public class SimpleQuery {

  private final Criteria criteria;
  private Pageable pageable;
  private Sort sort;
  private final List<String> filterQueries = new ArrayList<>();
  private final List<String> projectionFields = new ArrayList<>();
  private String requestHandler;
  private String defType;

  public SimpleQuery(Criteria criteria) {
    this.criteria = criteria;
  }

  public SimpleQuery(Criteria criteria, Pageable pageable) {
    this.criteria = criteria;
    this.pageable = pageable;
  }

  public SimpleQuery addFilterQuery(String fq) {
    filterQueries.add(fq);
    return this;
  }

  public SimpleQuery addProjectionOnField(String field) {
    projectionFields.add(field);
    return this;
  }

  public SimpleQuery setRequestHandler(String handler) {
    this.requestHandler = handler;
    return this;
  }

  public SimpleQuery setDefType(String defType) {
    this.defType = defType;
    return this;
  }

  public SimpleQuery setSort(Sort sort) {
    this.sort = sort;
    return this;
  }

  /** Converts this query into a SolrJ {@link SolrQuery} ready for execution. */
  public SolrQuery toSolrQuery() {
    var solrQuery = new SolrQuery(criteria.toQueryString());

    applyPagination(solrQuery);
    applySort(solrQuery);
    applyFilterQueries(solrQuery);
    applyProjectionFields(solrQuery);
    applyRequestHandler(solrQuery);
    applyDefType(solrQuery);

    return solrQuery;
  }

  private void applyPagination(SolrQuery solrQuery) {
    if (pageable != null && pageable.isPaged()) {
      solrQuery.setStart((int) pageable.getOffset());
      solrQuery.setRows(pageable.getPageSize());
    }
  }

  private void applySort(SolrQuery solrQuery) {
    var effectiveSort = resolveSort();
    if (effectiveSort == null || effectiveSort.isUnsorted()) {
      return;
    }
    var sortClauses = effectiveSort.stream()
        .map(order -> order.getProperty() + " " + order.getDirection().name().toLowerCase())
        .collect(Collectors.joining(","));
    solrQuery.set("sort", sortClauses);
  }

  private Sort resolveSort() {
    if (sort != null) {
      return sort;
    }
    if (pageable != null && pageable.getSort().isSorted()) {
      return pageable.getSort();
    }
    return null;
  }

  private void applyFilterQueries(SolrQuery solrQuery) {
    if (!filterQueries.isEmpty()) {
      solrQuery.setFilterQueries(filterQueries.toArray(new String[0]));
    }
  }

  private void applyProjectionFields(SolrQuery solrQuery) {
    if (!projectionFields.isEmpty()) {
      solrQuery.setFields(projectionFields.toArray(new String[0]));
    }
  }

  private void applyRequestHandler(SolrQuery solrQuery) {
    if (requestHandler != null) {
      solrQuery.setRequestHandler(requestHandler);
    }
  }

  private void applyDefType(SolrQuery solrQuery) {
    if (defType != null) {
      solrQuery.set("defType", defType);
    }
  }
}
