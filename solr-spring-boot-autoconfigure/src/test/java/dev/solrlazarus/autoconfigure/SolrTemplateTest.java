package dev.solrlazarus.autoconfigure;

import java.io.IOException;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolrTemplateTest {

  private static final String COLLECTION = "test-collection";

  @Mock
  private SolrClient solrClient;

  private SolrTemplate template;

  @BeforeEach
  void setUp() {
    template = new SolrTemplate(solrClient);
  }

  static TestDocument document(String id) {
    var doc = new TestDocument();
    doc.id = id;
    return doc;
  }

  static class TestDocument {
    String id;
  }

  @Nested
  class Save {

    @Test
    void delegatesToSolrClientAddBean() throws Exception {
      var entity = document("1");
      var result = template.save(COLLECTION, entity);
      verify(solrClient).addBean(COLLECTION, entity);
      assertThat(result).isSameAs(entity);
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      var entity = document("1");
      when(solrClient.addBean(COLLECTION, entity)).thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.save(COLLECTION, entity))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      var entity = document("1");
      when(solrClient.addBean(COLLECTION, entity))
          .thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.save(COLLECTION, entity))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class SaveAll {

    @Test
    void delegatesToSolrClientAddBeans() throws Exception {
      var entities = List.of(document("1"), document("2"));
      var result = template.saveAll(COLLECTION, entities);
      verify(solrClient).addBeans(COLLECTION, entities);
      assertThat(result).containsExactlyElementsOf(entities);
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      var entities = List.of(document("1"));
      when(solrClient.addBeans(COLLECTION, entities)).thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.saveAll(COLLECTION, entities))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      var entities = List.of(document("1"));
      when(solrClient.addBeans(COLLECTION, entities))
          .thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.saveAll(COLLECTION, entities))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class FindById {

    @Test
    void returnsOptionalOfEntityWhenFound() throws Exception {
      var entity = document("42");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var result = template.findById(COLLECTION, "42", TestDocument.class);

      assertThat(result).isPresent().contains(entity);
    }

    @Test
    void returnsOptionalEmptyWhenNotFound() throws Exception {
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of());
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var result = template.findById(COLLECTION, "99", TestDocument.class);

      assertThat(result).isEmpty();
    }

    @Test
    void escapesSpecialCharactersInIdBeforeQuerying() throws Exception {
      var entity = document("doc:1");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var result = template.findById(COLLECTION, "doc:1", TestDocument.class);

      assertThat(result).isPresent();
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class)))
          .thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.findById(COLLECTION, "1", TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining("1")
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class)))
          .thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.findById(COLLECTION, "1", TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class Query {

    @Test
    void delegatesToSolrClientQueryAndReturnsBeans() throws Exception {
      var solrQuery = new SolrQuery("category:books");
      var entity = document("10");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(solrClient.query(COLLECTION, solrQuery)).thenReturn(response);

      var result = template.query(COLLECTION, solrQuery, TestDocument.class);

      assertThat(result).containsExactly(entity);
    }

    @Test
    void returnsEmptyListWhenNoResultsFound() throws Exception {
      var solrQuery = new SolrQuery("category:nothing");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of());
      when(solrClient.query(COLLECTION, solrQuery)).thenReturn(response);

      var result = template.query(COLLECTION, solrQuery, TestDocument.class);

      assertThat(result).isEmpty();
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      var solrQuery = new SolrQuery("*:*");
      when(solrClient.query(COLLECTION, solrQuery)).thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.query(COLLECTION, solrQuery, TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      var solrQuery = new SolrQuery("*:*");
      when(solrClient.query(COLLECTION, solrQuery))
          .thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.query(COLLECTION, solrQuery, TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class DeleteById {

    @Test
    void delegatesToSolrClientDeleteById() throws Exception {
      template.deleteById(COLLECTION, "5");
      verify(solrClient).deleteById(COLLECTION, "5");
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.deleteById(COLLECTION, "5")).thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.deleteById(COLLECTION, "5"))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining("5")
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      when(solrClient.deleteById(COLLECTION, "5"))
          .thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.deleteById(COLLECTION, "5"))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class DeleteByQuery {

    @Test
    void delegatesToSolrClientDeleteByQuery() throws Exception {
      template.deleteByQuery(COLLECTION, "status:inactive");
      verify(solrClient).deleteByQuery(COLLECTION, "status:inactive");
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.deleteByQuery(COLLECTION, "status:inactive"))
          .thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.deleteByQuery(COLLECTION, "status:inactive"))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      when(solrClient.deleteByQuery(COLLECTION, "status:inactive"))
          .thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.deleteByQuery(COLLECTION, "status:inactive"))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class Commit {

    @Test
    void delegatesToSolrClientCommit() throws Exception {
      template.commit(COLLECTION);
      verify(solrClient).commit(COLLECTION);
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.commit(COLLECTION)).thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.commit(COLLECTION))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void wrapsSolrServerExceptionInSolrException() throws Exception {
      when(solrClient.commit(COLLECTION)).thenThrow(new SolrServerException("server error"));
      assertThatThrownBy(() -> template.commit(COLLECTION))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);
    }
  }

  @Nested
  class GetSolrClient {

    @Test
    void returnsTheWrappedSolrClient() {
      assertThat(template.getSolrClient()).isSameAs(solrClient);
    }
  }

  @Nested
  class EscapeQueryChars {

    @Test
    void escapesColonInId() {
      assertThat(SolrTemplate.escapeQueryChars("doc:1")).isEqualTo("doc\\:1");
    }

    @Test
    void escapesForwardSlashInId() {
      assertThat(SolrTemplate.escapeQueryChars("path/to")).isEqualTo("path\\/to");
    }

    @Test
    void escapesSpaceInId() {
      assertThat(SolrTemplate.escapeQueryChars("hello world")).isEqualTo("hello\\ world");
    }

    @Test
    void plainIdRequiresNoEscaping() {
      assertThat(SolrTemplate.escapeQueryChars("abc123")).isEqualTo("abc123");
    }

    @Test
    void escapesMultipleSpecialCharacters() {
      assertThat(SolrTemplate.escapeQueryChars("a+b-c")).isEqualTo("a\\+b\\-c");
    }
  }
}
