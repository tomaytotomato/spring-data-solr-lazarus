package com.tomaytotomato.data.solr;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "spring.solr")
public class SolrProperties {

  private final String host;
  private final String defaultCollection;
  private final String zkHost;
  private final Duration connectionTimeout;
  private final Duration requestTimeout;
  private final CommitMode commitMode;

  public SolrProperties(
      @DefaultValue("http://localhost:8983/solr") String host,
      String defaultCollection,
      String zkHost,
      @DefaultValue("10s") Duration connectionTimeout,
      @DefaultValue("60s") Duration requestTimeout,
      @DefaultValue("NONE") CommitMode commitMode) {
    this.host = host;
    this.defaultCollection = defaultCollection;
    this.zkHost = zkHost;
    this.connectionTimeout = connectionTimeout;
    this.requestTimeout = requestTimeout;
    this.commitMode = commitMode;
  }

  public String getHost() {
    return host;
  }

  public String getDefaultCollection() {
    return defaultCollection;
  }

  public String getZkHost() {
    return zkHost;
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
}
