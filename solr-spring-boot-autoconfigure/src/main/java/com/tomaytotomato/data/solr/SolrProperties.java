package com.tomaytotomato.data.solr;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.solr")
public class SolrProperties {

  private String host = "http://localhost:8983/solr";
  private String defaultCollection;
  private String zkHost;
  private Duration connectionTimeout = Duration.ofSeconds(10);
  private Duration requestTimeout = Duration.ofSeconds(60);
  private CommitMode commitMode = CommitMode.NONE;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getDefaultCollection() {
    return defaultCollection;
  }

  public void setDefaultCollection(String defaultCollection) {
    this.defaultCollection = defaultCollection;
  }

  public String getZkHost() {
    return zkHost;
  }

  public void setZkHost(String zkHost) {
    this.zkHost = zkHost;
  }

  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Duration connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public CommitMode getCommitMode() {
    return commitMode;
  }

  public void setCommitMode(CommitMode commitMode) {
    this.commitMode = commitMode;
  }
}
