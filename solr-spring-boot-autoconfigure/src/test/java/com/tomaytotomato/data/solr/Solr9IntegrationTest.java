package com.tomaytotomato.data.solr;

import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Solr 9 Integration Tests")
class Solr9IntegrationTest extends AbstractSolrIntegrationTest {

  @Container
  static final SolrContainer solr = new SolrContainer(DockerImageName.parse("solr:9"))
      .withCollection(COLLECTION);

  @Override
  SolrContainer getSolrContainer() {
    return solr;
  }
}
