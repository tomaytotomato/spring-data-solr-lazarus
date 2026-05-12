package dev.solrlazarus.autoconfigure;

import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SolrPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class));

  @Nested
  class Defaults {

    @Test
    void hostDefaultsToLocalhost() {
      contextRunner.run(ctx -> {
        var properties = ctx.getBean(SolrProperties.class);
        assertThat(properties.getHost()).isEqualTo("http://localhost:8983/solr");
      });
    }

    @Test
    void connectionTimeoutDefaultsToTenSeconds() {
      contextRunner.run(ctx -> {
        var properties = ctx.getBean(SolrProperties.class);
        assertThat(properties.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(10));
      });
    }

    @Test
    void requestTimeoutDefaultsToSixtySeconds() {
      contextRunner.run(ctx -> {
        var properties = ctx.getBean(SolrProperties.class);
        assertThat(properties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
      });
    }

    @Test
    void defaultCollectionIsNull() {
      contextRunner.run(ctx -> {
        var properties = ctx.getBean(SolrProperties.class);
        assertThat(properties.getDefaultCollection()).isNull();
      });
    }

    @Test
    void zkHostIsNull() {
      contextRunner.run(ctx -> {
        var properties = ctx.getBean(SolrProperties.class);
        assertThat(properties.getZkHost()).isNull();
      });
    }
  }

  @Nested
  class CustomBinding {

    @Test
    void customHostBindsFromProperty() {
      contextRunner
          .withPropertyValues("spring.solr.host=http://solr.example.com:8983/solr")
          .run(ctx -> {
            var properties = ctx.getBean(SolrProperties.class);
            assertThat(properties.getHost()).isEqualTo("http://solr.example.com:8983/solr");
          });
    }

    @Test
    void customDefaultCollectionBindsFromProperty() {
      contextRunner
          .withPropertyValues("spring.solr.default-collection=my-collection")
          .run(ctx -> {
            var properties = ctx.getBean(SolrProperties.class);
            assertThat(properties.getDefaultCollection()).isEqualTo("my-collection");
          });
    }

    @Test
    void durationStyleConnectionTimeoutBindsFromProperty() {
      contextRunner
          .withPropertyValues("spring.solr.connection-timeout=5s")
          .run(ctx -> {
            var properties = ctx.getBean(SolrProperties.class);
            assertThat(properties.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(5));
          });
    }

    @Test
    void durationStyleRequestTimeoutBindsFromProperty() {
      contextRunner
          .withPropertyValues("spring.solr.request-timeout=30s")
          .run(ctx -> {
            var properties = ctx.getBean(SolrProperties.class);
            assertThat(properties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(30));
          });
    }
  }
}
