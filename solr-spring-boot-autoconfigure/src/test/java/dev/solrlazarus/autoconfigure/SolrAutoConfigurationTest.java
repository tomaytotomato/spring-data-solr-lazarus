package dev.solrlazarus.autoconfigure;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SolrAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class));

  @Nested
  class BeanCreation {

    @Test
    void createsSolrClientWhenSolrClientClassIsOnClasspath() {
      contextRunner.run(ctx ->
          assertThat(ctx).hasSingleBean(SolrClient.class));
    }

    @Test
    void createsSolrTemplateWhenSolrClientClassIsOnClasspath() {
      contextRunner.run(ctx ->
          assertThat(ctx).hasSingleBean(SolrTemplate.class));
    }

    @Test
    void solrTemplateIsWiredWithTheSolrClient() {
      contextRunner.run(ctx -> {
        var template = ctx.getBean(SolrTemplate.class);
        var client = ctx.getBean(SolrClient.class);
        assertThat(template.getSolrClient()).isSameAs(client);
      });
    }
  }

  @Nested
  class PropertyApplication {

    @Test
    void customHostPropertyIsReflectedInProperties() {
      contextRunner
          .withPropertyValues("spring.solr.host=http://custom-solr:8983/solr")
          .run(ctx -> {
            var properties = ctx.getBean(SolrProperties.class);
            assertThat(properties.getHost()).isEqualTo("http://custom-solr:8983/solr");
          });
    }

    @Test
    void customCollectionPropertyIsReflectedInProperties() {
      contextRunner
          .withPropertyValues("spring.solr.default-collection=products")
          .run(ctx -> {
            var properties = ctx.getBean(SolrProperties.class);
            assertThat(properties.getDefaultCollection()).isEqualTo("products");
          });
    }
  }

  @Nested
  class UserProvidedBeans {

    @Test
    void userProvidedSolrClientTakesPrecedence() {
      var userClient = mock(SolrClient.class);
      contextRunner
          .withBean("customSolrClient", SolrClient.class, () -> userClient)
          .run(ctx -> {
            assertThat(ctx).hasSingleBean(SolrClient.class);
            assertThat(ctx.getBean(SolrClient.class)).isSameAs(userClient);
          });
    }

    @Test
    void userProvidedSolrTemplateTakesPrecedence() {
      var userClient = mock(SolrClient.class);
      var userTemplate = new SolrTemplate(userClient);
      contextRunner
          .withBean("customSolrClient", SolrClient.class, () -> userClient)
          .withBean("customSolrTemplate", SolrTemplate.class, () -> userTemplate)
          .run(ctx -> {
            assertThat(ctx).hasSingleBean(SolrTemplate.class);
            assertThat(ctx.getBean(SolrTemplate.class)).isSameAs(userTemplate);
          });
    }
  }
}
