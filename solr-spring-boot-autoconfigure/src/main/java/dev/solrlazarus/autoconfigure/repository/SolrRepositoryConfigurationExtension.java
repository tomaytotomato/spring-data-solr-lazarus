package dev.solrlazarus.autoconfigure.repository;

import dev.solrlazarus.autoconfigure.mapping.SolrDocument;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

public class SolrRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

  @Override
  public String getModuleName() {
    return "Solr";
  }

  @Override
  protected String getModulePrefix() {
    return "solr";
  }

  @Override
  public String getRepositoryFactoryBeanClassName() {
    return SolrRepositoryFactoryBean.class.getName();
  }

  @Override
  protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
    return Collections.singleton(SolrDocument.class);
  }

  @Override
  protected Collection<Class<?>> getIdentifyingTypes() {
    return Collections.singleton(SolrRepository.class);
  }
}
