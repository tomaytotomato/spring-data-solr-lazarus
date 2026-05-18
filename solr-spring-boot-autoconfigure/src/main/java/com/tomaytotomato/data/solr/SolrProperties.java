package com.tomaytotomato.data.solr;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "spring.solr")
public class SolrProperties {

  private final StandaloneProperties standalone;
  private final CloudProperties cloud;
  private final Duration connectionTimeout;
  private final Duration requestTimeout;
  private final CommitMode commitMode;

  public SolrProperties(
      StandaloneProperties standalone,
      CloudProperties cloud,
      @DefaultValue("10s") Duration connectionTimeout,
      @DefaultValue("60s") Duration requestTimeout,
      @DefaultValue("NONE") CommitMode commitMode) {
    this.standalone = standalone;
    this.cloud = cloud;
    this.connectionTimeout = connectionTimeout;
    this.requestTimeout = requestTimeout;
    this.commitMode = commitMode;
  }

  public StandaloneProperties getStandalone() {
    return standalone;
  }

  public CloudProperties getCloud() {
    return cloud;
  }

  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public CommitMode getCommitMode() {
    return commitMode;
  }

  public String getDefaultCollection() {
    if (cloud != null) {
      return cloud.defaultCollection();
    }
    if (standalone != null) {
      return standalone.defaultCollection();
    }
    return null;
  }

  public record StandaloneProperties(
      @DefaultValue("http://localhost:8983/solr") String host,
      String defaultCollection) {}

  public record CloudProperties(
      String zkHost,
      String defaultCollection) {}
}
