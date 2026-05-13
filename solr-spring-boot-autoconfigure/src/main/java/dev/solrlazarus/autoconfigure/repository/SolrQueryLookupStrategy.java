package dev.solrlazarus.autoconfigure.repository;

import dev.solrlazarus.autoconfigure.SolrTemplate;
import java.lang.reflect.Method;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

public class SolrQueryLookupStrategy implements QueryLookupStrategy {

  private final SolrTemplate solrTemplate;

  public SolrQueryLookupStrategy(SolrTemplate solrTemplate) {
    this.solrTemplate = solrTemplate;
  }

  @Override
  public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
      ProjectionFactory factory, NamedQueries namedQueries) {
    var queryMethod = new QueryMethod(method, metadata, factory);
    return new PartTreeSolrQuery(queryMethod, solrTemplate);
  }
}
