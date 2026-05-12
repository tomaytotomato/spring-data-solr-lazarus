package dev.solrlazarus.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.solr")
public class SolrProperties {

  private String host = "http://localhost:8983/solr";
  private String defaultCollection;
  private String zkHost;
  private int connectionTimeout = 10_000;
  private int requestTimeout = 60_000;

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

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }
}
