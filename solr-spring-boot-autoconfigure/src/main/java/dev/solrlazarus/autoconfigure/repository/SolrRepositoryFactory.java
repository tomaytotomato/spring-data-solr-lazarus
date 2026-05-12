package dev.solrlazarus.autoconfigure.repository;

import dev.solrlazarus.autoconfigure.SolrTemplate;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class SolrRepositoryFactory extends RepositoryFactorySupport {

  private final SolrTemplate solrTemplate;

  public SolrRepositoryFactory(SolrTemplate solrTemplate) {
    this.solrTemplate = solrTemplate;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
    return new SolrEntityInformation<>(domainClass, (Class<ID>) String.class);
  }

  @Override
  protected Object getTargetRepository(RepositoryInformation information) {
    return getTargetRepositoryViaReflection(information, solrTemplate, information.getDomainType());
  }

  @Override
  protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
    return SimpleSolrRepository.class;
  }
}
