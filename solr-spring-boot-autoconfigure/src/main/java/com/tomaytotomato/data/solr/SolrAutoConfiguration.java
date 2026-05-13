package com.tomaytotomato.data.solr;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnClass(SolrClient.class)
@EnableConfigurationProperties(SolrProperties.class)
public class SolrAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = "spring.solr", name = "zk-host")
  static class CloudSolrClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(SolrClient.class)
    SolrClient cloudSolrClient(SolrProperties properties) {
      return new CloudSolrClient.Builder(List.of(properties.getZkHost()))
          .withDefaultCollection(properties.getDefaultCollection())
          .build();
    }
  }

  @Bean
  @ConditionalOnMissingBean(SolrClient.class)
  public SolrClient solrClient(SolrProperties properties) {
    return new HttpJdkSolrClient.Builder(properties.getHost())
        .withConnectionTimeout(
            properties.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .withRequestTimeout(
            properties.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .withDefaultCollection(properties.getDefaultCollection())
        .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public SolrTemplate solrTemplate(SolrClient solrClient, SolrProperties properties) {
    return new SolrTemplate(solrClient, properties.getCommitMode());
  }
}
