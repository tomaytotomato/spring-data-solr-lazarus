package dev.solrlazarus.autoconfigure.health;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

public class SolrHealthIndicator extends AbstractHealthIndicator {

  private final SolrClient solrClient;

  public SolrHealthIndicator(SolrClient solrClient) {
    super("Solr health check failed");
    this.solrClient = solrClient;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    SolrPingResponse response = solrClient.ping();
    int status = response.getStatus();
    if (status == 0) {
      builder.up()
          .withDetail("status", status)
          .withDetail("elapsed_time_ms", response.getElapsedTime());
    } else {
      builder.down().withDetail("status", status);
    }
  }
}
