package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.FacetPage;
import com.tomaytotomato.data.solr.HighlightPage;
import com.tomaytotomato.data.solr.SolrTemplate;
import com.tomaytotomato.data.solr.mapping.SolrDocumentResolver;
import java.lang.reflect.Method;
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
  private final Method method;

  public PartTreeSolrQuery(QueryMethod queryMethod, SolrTemplate solrTemplate, Method method) {
    this.queryMethod = queryMethod;
    this.solrTemplate = solrTemplate;
    this.domainType = queryMethod.getEntityInformation().getJavaType();
    this.tree = new PartTree(queryMethod.getName(), domainType);
    this.method = method;
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

    var highlightAnnotation = method.getAnnotation(Highlight.class);
    if (highlightAnnotation != null && HighlightPage.class.isAssignableFrom(method.getReturnType())) {
      var pageable = accessor.getPageable().isUnpaged()
          ? PageRequest.of(0, 10) : accessor.getPageable();
      query.setPageable(pageable);
      query.setHighlightOptions(HighlightAnnotationAdapter.toHighlightOptions(highlightAnnotation));
      return solrTemplate.queryForHighlightPage(collection, query, domainType);
    }

    var facetAnnotation = method.getAnnotation(Facet.class);
    if (facetAnnotation != null && FacetPage.class.isAssignableFrom(method.getReturnType())) {
      var pageable = accessor.getPageable().isUnpaged()
          ? PageRequest.of(0, 10) : accessor.getPageable();
      query.setPageable(pageable);
      query.setFacetOptions(FacetAnnotationAdapter.toFacetOptions(facetAnnotation));
      return solrTemplate.queryForFacetPage(collection, query, domainType);
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
