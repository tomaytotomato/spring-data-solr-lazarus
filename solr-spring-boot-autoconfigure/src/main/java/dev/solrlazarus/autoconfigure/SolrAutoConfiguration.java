package dev.solrlazarus.autoconfigure;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(SolrClient.class)
@EnableConfigurationProperties(SolrProperties.class)
public class SolrAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = "spring.solr", name = "zk-host", matchIfMissing = false)
  public SolrClient cloudSolrClient(SolrProperties properties) {
    try {
      var builderClass =
          Class.forName("org.apache.solr.client.solrj.impl.CloudSolrClient$Builder");
      var constructor = builderClass.getConstructor(List.class, Optional.class);
      var builder = constructor.newInstance(List.of(properties.getZkHost()), Optional.empty());

      var buildMethod = builderClass.getMethod("build");
      var client = (SolrClient) buildMethod.invoke(builder);

      Optional.ofNullable(properties.getDefaultCollection())
          .ifPresent(
              collection -> {
                try {
                  var setMethod =
                      client.getClass().getMethod("setDefaultCollection", String.class);
                  setMethod.invoke(client, collection);
                } catch (Exception e) {
                  throw new IllegalStateException("Failed to set default collection", e);
                }
              });

      return client;
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "CloudSolrClient requires solr-solrj-zookeeper on the classpath", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create CloudSolrClient", e);
    }
  }

  @Bean
  @ConditionalOnMissingBean(SolrClient.class)
  public SolrClient solrClient(SolrProperties properties) {
    return new HttpJdkSolrClient.Builder(properties.getHost())
        .withConnectionTimeout(properties.getConnectionTimeout(), TimeUnit.MILLISECONDS)
        .withRequestTimeout(properties.getRequestTimeout(), TimeUnit.MILLISECONDS)
        .withDefaultCollection(properties.getDefaultCollection())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public SolrTemplate solrTemplate(SolrClient solrClient) {
    return new SolrTemplate(solrClient);
  }
}
