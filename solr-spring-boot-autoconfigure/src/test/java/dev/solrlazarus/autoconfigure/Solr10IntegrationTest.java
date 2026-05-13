package dev.solrlazarus.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Solr 10 Integration Tests")
class Solr10IntegrationTest extends AbstractSolrIntegrationTest {

  @Container
  static final SolrContainer solr = new SolrContainer(DockerImageName.parse("solr:10"))
      .withCollection(COLLECTION);

  @Override
  SolrContainer getSolrContainer() {
    return solr;
  }
}
