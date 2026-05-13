package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.SolrTemplate;
import com.tomaytotomato.data.solr.mapping.SolrDocumentResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeSolrQuery implements RepositoryQuery {

  private final QueryMethod queryMethod;
  private final SolrTemplate solrTemplate;
  private final PartTree tree;
  private final Class<?> domainType;

  public PartTreeSolrQuery(QueryMethod queryMethod, SolrTemplate solrTemplate) {
    this.queryMethod = queryMethod;
    this.solrTemplate = solrTemplate;
    this.domainType = queryMethod.getEntityInformation().getJavaType();
    this.tree = new PartTree(queryMethod.getName(), domainType);
  }

  @Override
  public Object execute(Object[] parameters) {
    var accessor = new ParametersParameterAccessor(queryMethod.getParameters(), parameters);
    var resolver = SolrFieldNameResolver.forClass(domainType);
    var query = new SolrQueryCreator(tree, accessor, resolver).createQuery();
    var collection = SolrDocumentResolver.resolveCollection(domainType);

    if (tree.isCountProjection()) {
      return solrTemplate.count(collection, query);
    }

    if (tree.isExistsProjection()) {
      return solrTemplate.count(collection, query) > 0;
    }

    if (queryMethod.isPageQuery()) {
      var pageable = accessor.getPageable().isUnpaged()
          ? PageRequest.of(0, 10) : accessor.getPageable();
      query.setPageable(pageable);
      return solrTemplate.queryForPage(collection, query, domainType);
    }

    if (queryMethod.isCollectionQuery()) {
      return solrTemplate.query(collection, query.toSolrQuery(), domainType);
    }

    var results = solrTemplate.query(collection, query.toSolrQuery(), domainType);
    return results.isEmpty() ? null : results.getFirst();
  }

  @Override
  public QueryMethod getQueryMethod() {
    return queryMethod;
  }
}
