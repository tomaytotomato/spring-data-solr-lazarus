package com.tomaytotomato.data.solr.health;

import com.tomaytotomato.data.solr.SolrAutoConfiguration;
import com.tomaytotomato.data.solr.SolrProperties;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = SolrAutoConfiguration.class)
@ConditionalOnClass({SolrClient.class, HealthIndicator.class})
@ConditionalOnBean(SolrClient.class)
@ConditionalOnEnabledHealthIndicator("solr")
public class SolrHealthAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "solrHealthIndicator")
  public SolrHealthIndicator solrHealthIndicator(
      SolrClient solrClient, SolrProperties properties) {
    return new SolrHealthIndicator(solrClient, properties.getDefaultCollection());
  }
}
