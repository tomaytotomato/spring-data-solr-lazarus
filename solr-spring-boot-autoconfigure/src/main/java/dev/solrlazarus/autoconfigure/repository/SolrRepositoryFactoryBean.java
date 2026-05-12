package dev.solrlazarus.autoconfigure.repository;

import dev.solrlazarus.autoconfigure.SolrTemplate;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class SolrRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
    extends RepositoryFactoryBeanSupport<T, S, ID> {

  private SolrTemplate solrTemplate;

  protected SolrRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
    super(repositoryInterface);
  }

  public void setSolrTemplate(SolrTemplate solrTemplate) {
    this.solrTemplate = solrTemplate;
  }

  @Override
  protected RepositoryFactorySupport createRepositoryFactory() {
    return new SolrRepositoryFactory(solrTemplate);
  }
}
