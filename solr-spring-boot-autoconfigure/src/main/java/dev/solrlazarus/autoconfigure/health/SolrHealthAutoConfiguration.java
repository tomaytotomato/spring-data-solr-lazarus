package dev.solrlazarus.autoconfigure.health;

import dev.solrlazarus.autoconfigure.SolrAutoConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = SolrAutoConfiguration.class)
@ConditionalOnClass({SolrClient.class, HealthIndicator.class})
@ConditionalOnBean(SolrClient.class)
public class SolrHealthAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "solrHealthIndicator")
  public SolrHealthIndicator solrHealthIndicator(SolrClient solrClient) {
    return new SolrHealthIndicator(solrClient);
  }
}
