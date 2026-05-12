package dev.solrlazarus.autoconfigure.health;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.util.NamedList;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

public class SolrHealthIndicator extends AbstractHealthIndicator {

  private final SolrClient solrClient;
  private final String collection;

  public SolrHealthIndicator(SolrClient solrClient, String collection) {
    super("Solr health check failed");
    this.solrClient = solrClient;
    this.collection = collection;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    if (collection != null) {
      SolrPingResponse response = solrClient.ping(collection);
      int status = response.getStatus();
      if (status == 0) {
        builder.up()
            .withDetail("collection", collection)
            .withDetail("status", status)
            .withDetail("elapsed_time_ms", response.getElapsedTime());
      } else {
        builder.down()
            .withDetail("collection", collection)
            .withDetail("status", status);
      }
    } else {
      var request = new GenericSolrRequest(
          SolrRequest.METHOD.GET, "/admin/info/system", SolrRequest.SolrRequestType.ADMIN);
      var response = request.process(solrClient);
      NamedList<Object> result = response.getResponse();
      var solrVersion = result.get("lucene") instanceof NamedList<?> lucene
          ? lucene.get("solr-spec-version") : "unknown";
      builder.up().withDetail("solr-version", solrVersion);
    }
  }
}
