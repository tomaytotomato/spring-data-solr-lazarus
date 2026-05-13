package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.SolrTemplate;
import com.tomaytotomato.data.solr.mapping.SolrDocumentResolver;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

public class StringBasedSolrQuery implements RepositoryQuery {

  private final QueryMethod queryMethod;
  private final SolrTemplate solrTemplate;
  private final String queryString;
  private final boolean isCountQuery;
  private final Class<?> domainType;

  public StringBasedSolrQuery(QueryMethod queryMethod, SolrTemplate solrTemplate,
      String queryString, boolean isCountQuery) {
    this.queryMethod = queryMethod;
    this.solrTemplate = solrTemplate;
    this.queryString = queryString;
    this.isCountQuery = isCountQuery;
    this.domainType = queryMethod.getEntityInformation().getJavaType();
  }

  @Override
  public Object execute(Object[] parameters) {
    var resolvedQuery = resolveParameters(queryString, parameters);
    var collection = SolrDocumentResolver.resolveCollection(domainType);
    var solrQuery = new SolrQuery(resolvedQuery);

    if (isCountQuery) {
      return solrTemplate.count(collection, solrQuery);
    }

    if (queryMethod.isCollectionQuery()) {
      return solrTemplate.query(collection, solrQuery, domainType);
    }

    var results = solrTemplate.query(collection, solrQuery, domainType);
    return results.isEmpty() ? null : results.getFirst();
  }

  private String resolveParameters(String query, Object[] parameters) {
    var resolved = query;
    for (int i = 0; i < parameters.length; i++) {
      resolved = resolved.replace("?" + i, String.valueOf(parameters[i]));
    }
    return resolved;
  }

  @Override
  public QueryMethod getQueryMethod() {
    return queryMethod;
  }
}
