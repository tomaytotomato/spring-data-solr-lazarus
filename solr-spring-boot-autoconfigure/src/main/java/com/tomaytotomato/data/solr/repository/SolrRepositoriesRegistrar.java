package com.tomaytotomato.data.solr.repository;

import java.lang.annotation.Annotation;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

public class SolrRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

  @Override
  protected Class<? extends Annotation> getAnnotation() {
    return EnableSolrRepositories.class;
  }

  @Override
  protected Class<?> getConfiguration() {
    return EnableSolrRepositoriesConfiguration.class;
  }

  @Override
  protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
    return new SolrRepositoryConfigurationExtension();
  }

  @EnableSolrRepositories
  private static class EnableSolrRepositoriesConfiguration {
  }
}
