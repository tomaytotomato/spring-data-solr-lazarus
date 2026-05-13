package dev.solrlazarus.autoconfigure;

import dev.solrlazarus.autoconfigure.mapping.SolrDocument;
import dev.solrlazarus.autoconfigure.query.Criteria;
import dev.solrlazarus.autoconfigure.query.HighlightOptions;
import dev.solrlazarus.autoconfigure.query.SimpleQuery;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

  @SolrDocument(collection = "annotated-collection")
  static class AnnotatedDocument {
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
  class SaveWithAnnotatedCollection {

    @Test
    void resolvesCollectionFromSolrDocumentAnnotation() throws Exception {
      var entity = new AnnotatedDocument();
      entity.id = "1";

      var result = template.save(entity);

      verify(solrClient).addBean("annotated-collection", entity);
      assertThat(result).isSameAs(entity);
    }
  }

  @Nested
  class FindByIdWithAnnotatedCollection {

    @Test
    void resolvesCollectionFromSolrDocumentAnnotation() throws Exception {
      var entity = new AnnotatedDocument();
      entity.id = "42";
      var response = mock(QueryResponse.class);
      when(response.getBeans(AnnotatedDocument.class)).thenReturn(List.of(entity));
      when(solrClient.query(eq("annotated-collection"), any(SolrParams.class))).thenReturn(response);

      var result = template.findById("42", AnnotatedDocument.class);

      assertThat(result).isPresent().contains(entity);
    }
  }

  @Nested
  class SoftCommit {

    @Test
    void delegatesToSolrClientSoftCommit() throws Exception {
      template.softCommit(COLLECTION);
      verify(solrClient).commit(COLLECTION, true, true, true);
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.commit(COLLECTION, true, true, true)).thenThrow(new IOException("network error"));
      assertThatThrownBy(() -> template.softCommit(COLLECTION))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @Nested
  class CommitModeImmediate {

    @Test
    void commitsAfterSaveWhenModeIsImmediate() throws Exception {
      var immediateTemplate = new SolrTemplate(solrClient, CommitMode.IMMEDIATE);
      var entity = document("1");

      immediateTemplate.save(COLLECTION, entity);

      verify(solrClient).addBean(COLLECTION, entity);
      verify(solrClient).commit(COLLECTION);
    }

    @Test
    void doesNotCommitAfterSaveWhenModeIsNone() throws Exception {
      var entity = document("1");

      template.save(COLLECTION, entity);

      verify(solrClient).addBean(COLLECTION, entity);
      verify(solrClient, never()).commit(COLLECTION);
    }
  }

  @Nested
  class QueryWithCursor {

    @Test
    void returnsContentWithNextCursorMarkAndHasMoreTrueWhenCursorAdvances() throws Exception {
      var entity = document("1");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(response.getNextCursorMark()).thenReturn("AoE=");
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setCursorMark("*");

      var result = template.queryWithCursor(COLLECTION, query, TestDocument.class);

      assertThat(result.content()).containsExactly(entity);
      assertThat(result.cursorMark()).isEqualTo("AoE=");
      assertThat(result.hasMore()).isTrue();
    }

    @Test
    void hasMoreIsFalseWhenNextCursorMarkEqualsRequestCursorMark() throws Exception {
      var entity = document("2");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(response.getNextCursorMark()).thenReturn("AoE=");
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setCursorMark("AoE=");

      var result = template.queryWithCursor(COLLECTION, query, TestDocument.class);

      assertThat(result.hasMore()).isFalse();
      assertThat(result.cursorMark()).isEqualTo("AoE=");
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class)))
          .thenThrow(new IOException("network error"));

      var query = new SimpleQuery(Criteria.where("*").is("*"))
          .setCursorMark("*");

      assertThatThrownBy(() -> template.queryWithCursor(COLLECTION, query, TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @Nested
  class QueryForHighlightPage {

    @Test
    void returnsHighlightEntriesPairedWithEntities() throws Exception {
      var entity = document("1");
      var solrDoc = new org.apache.solr.common.SolrDocument();
      solrDoc.setField("id", "1");
      var docList = new SolrDocumentList();
      docList.add(solrDoc);
      docList.setNumFound(1L);

      var highlights = Map.of("1", Map.of("title", List.of("a <em>match</em>")));

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(response.getResults()).thenReturn(docList);
      when(response.getHighlighting()).thenReturn(highlights);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var options = new HighlightOptions().addField("title");
      var query = new SimpleQuery(Criteria.where("*").is("*")).setHighlightOptions(options);

      var result = template.queryForHighlightPage(COLLECTION, query, TestDocument.class);

      assertThat(result.getHighlighted()).hasSize(1);
      assertThat(result.getHighlighted().get(0).entity()).isSameAs(entity);
      assertThat(result.getHighlighted().get(0).highlights())
          .containsKey("title")
          .extractingByKey("title")
          .asList()
          .containsExactly("a <em>match</em>");
    }

    @Test
    void returnsEmptyHighlightsWhenNoHighlightingInResponse() throws Exception {
      var entity = document("2");
      var solrDoc = new org.apache.solr.common.SolrDocument();
      solrDoc.setField("id", "2");
      var docList = new SolrDocumentList();
      docList.add(solrDoc);
      docList.setNumFound(1L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(response.getResults()).thenReturn(docList);
      when(response.getHighlighting()).thenReturn(null);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var result = template.queryForHighlightPage(COLLECTION, query, TestDocument.class);

      assertThat(result.getHighlighted()).hasSize(1);
      assertThat(result.getHighlighted().get(0).highlights()).isEmpty();
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class)))
          .thenThrow(new IOException("network error"));

      var query = new SimpleQuery(Criteria.where("*").is("*"));

      assertThatThrownBy(() -> template.queryForHighlightPage(COLLECTION, query, TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @Nested
  class QueryForFacetPage {

    @Test
    void returnsFieldFacetsParsedFromResponse() throws Exception {
      var entity = document("1");
      var docList = new SolrDocumentList();
      docList.setNumFound(1L);

      var genreField = new org.apache.solr.client.solrj.response.FacetField("genre");
      genreField.add("sci-fi", 42L);
      genreField.add("drama", 17L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of(entity));
      when(response.getResults()).thenReturn(docList);
      when(response.getFacetFields()).thenReturn(List.of(genreField));
      when(response.getFacetQuery()).thenReturn(Map.of());
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var result = template.queryForFacetPage(COLLECTION, query, TestDocument.class);

      assertThat(result.getFacetField("genre")).hasSize(2);
      assertThat(result.getFacetField("genre").get(0).value()).isEqualTo("sci-fi");
      assertThat(result.getFacetField("genre").get(0).count()).isEqualTo(42L);
      assertThat(result.getFacetField("genre").get(1).value()).isEqualTo("drama");
      assertThat(result.getFacetField("genre").get(1).count()).isEqualTo(17L);
    }

    @Test
    void returnsQueryFacetsParsedFromResponse() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(0L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of());
      when(response.getResults()).thenReturn(docList);
      when(response.getFacetFields()).thenReturn(List.of());
      when(response.getFacetQuery()).thenReturn(Map.of("price:[0 TO 10]", 23, "price:[10 TO 100]", 87));
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var result = template.queryForFacetPage(COLLECTION, query, TestDocument.class);

      assertThat(result.getFacetQueries()).hasSize(2);
      assertThat(result.getFacetQueries())
          .extracting(dev.solrlazarus.autoconfigure.query.FacetQueryEntry::query)
          .containsExactlyInAnyOrder("price:[0 TO 10]", "price:[10 TO 100]");
    }

    @Test
    void returnsEmptyFacetsWhenNullFacetFields() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(0L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDocument.class)).thenReturn(List.of());
      when(response.getResults()).thenReturn(docList);
      when(response.getFacetFields()).thenReturn(null);
      when(response.getFacetQuery()).thenReturn(null);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"));
      var result = template.queryForFacetPage(COLLECTION, query, TestDocument.class);

      assertThat(result.getFacetFields()).isEmpty();
      assertThat(result.getFacetQueries()).isEmpty();
    }

    @Test
    void wrapsIOExceptionInSolrException() throws Exception {
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class)))
          .thenThrow(new IOException("network error"));

      var query = new SimpleQuery(Criteria.where("*").is("*"));

      assertThatThrownBy(() -> template.queryForFacetPage(COLLECTION, query, TestDocument.class))
          .isInstanceOf(SolrException.class)
          .hasMessageContaining(COLLECTION)
          .hasCauseInstanceOf(IOException.class);
    }
  }
}
