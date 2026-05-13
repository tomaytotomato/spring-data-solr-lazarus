package com.tomaytotomato.data.solr;

import com.tomaytotomato.data.solr.query.Criteria;
import com.tomaytotomato.data.solr.query.SimpleQuery;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicrometerSolrTemplateTest {

  private static final String COLLECTION = "books";

  @Mock
  private SolrClient solrClient;

  private MeterRegistry meterRegistry;
  private MicrometerSolrTemplate template;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    template = new MicrometerSolrTemplate(solrClient, CommitMode.NONE, null, meterRegistry);
  }

  static TestDoc doc(String id) {
    var d = new TestDoc();
    d.id = id;
    return d;
  }

  static class TestDoc {
    String id;
  }

  private Timer timerFor(String operation) {
    return meterRegistry.find("solr.operations")
        .tag("operation", operation)
        .tag("collection", COLLECTION)
        .timer();
  }

  @Nested
  class QueryTimerRegistration {

    @Test
    void registersTimerAfterQueryOperation() throws Exception {
      var solrQuery = new SolrQuery("*:*");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of());
      when(solrClient.query(COLLECTION, solrQuery)).thenReturn(response);

      template.query(COLLECTION, solrQuery, TestDoc.class);

      assertThat(timerFor("query")).isNotNull();
      assertThat(timerFor("query").count()).isEqualTo(1);
    }

    @Test
    void queryResultPassesThroughTheTimerWrapper() throws Exception {
      var entity = doc("1");
      var solrQuery = new SolrQuery("title:foo");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of(entity));
      when(solrClient.query(COLLECTION, solrQuery)).thenReturn(response);

      var result = template.query(COLLECTION, solrQuery, TestDoc.class);

      assertThat(result).containsExactly(entity);
    }

    @Test
    void queryExceptionPropagatesThroughTimerWrapper() throws Exception {
      var solrQuery = new SolrQuery("*:*");
      when(solrClient.query(COLLECTION, solrQuery)).thenThrow(new IOException("network"));

      assertThatThrownBy(() -> template.query(COLLECTION, solrQuery, TestDoc.class))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(IOException.class);

      assertThat(timerFor("query")).isNotNull();
    }
  }

  @Nested
  class QueryForPageTimerRegistration {

    @Test
    void registersTimerAfterQueryForPageOperation() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(0L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of());
      when(response.getResults()).thenReturn(docList);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var query = new SimpleQuery(Criteria.where("*").is("*"));
      template.queryForPage(COLLECTION, query, TestDoc.class);

      assertThat(timerFor("queryForPage")).isNotNull();
      assertThat(timerFor("queryForPage").count()).isEqualTo(1);
    }

    @Test
    void queryForPageResultPassesThroughTimerWrapper() throws Exception {
      var entity = doc("2");
      var docList = new SolrDocumentList();
      docList.setNumFound(1L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of(entity));
      when(response.getResults()).thenReturn(docList);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var result = template.queryForPage(COLLECTION, new SimpleQuery(Criteria.where("*").is("*")), TestDoc.class);

      assertThat(result.getContent()).containsExactly(entity);
    }
  }

  @Nested
  class CountTimerRegistration {

    @Test
    void registersTimerAfterCountWithSolrQueryOperation() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(7L);
      var response = mock(QueryResponse.class);
      when(response.getResults()).thenReturn(docList);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      template.count(COLLECTION, new SolrQuery("*:*"));

      assertThat(timerFor("count")).isNotNull();
      assertThat(timerFor("count").count()).isEqualTo(1);
    }

    @Test
    void registersTimerAfterCountWithSimpleQueryOperation() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(3L);
      var response = mock(QueryResponse.class);
      when(response.getResults()).thenReturn(docList);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      template.count(COLLECTION, new SimpleQuery(Criteria.where("*").is("*")));

      assertThat(timerFor("count")).isNotNull();
    }

    @Test
    void countResultPassesThroughTimerWrapper() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(99L);
      var response = mock(QueryResponse.class);
      when(response.getResults()).thenReturn(docList);
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      var result = template.count(COLLECTION, new SolrQuery("*:*"));

      assertThat(result).isEqualTo(99L);
    }
  }

  @Nested
  class SaveTimerRegistration {

    @Test
    void registersTimerAfterSaveWithCollectionOperation() throws Exception {
      var entity = doc("1");
      template.save(COLLECTION, entity);

      assertThat(timerFor("save")).isNotNull();
      assertThat(timerFor("save").count()).isEqualTo(1);
    }

    @Test
    void saveResultPassesThroughTimerWrapper() throws Exception {
      var entity = doc("42");
      var result = template.save(COLLECTION, entity);

      assertThat(result).isSameAs(entity);
    }

    @Test
    void saveExceptionPropagatesThroughTimerWrapper() throws Exception {
      var entity = doc("1");
      when(solrClient.addBean(COLLECTION, entity)).thenThrow(new SolrServerException("boom"));

      assertThatThrownBy(() -> template.save(COLLECTION, entity))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(SolrServerException.class);

      assertThat(timerFor("save")).isNotNull();
    }
  }

  @Nested
  class SaveAllTimerRegistration {

    @Test
    void registersTimerAfterSaveAllOperation() throws Exception {
      var entities = List.of(doc("1"), doc("2"));
      template.saveAll(COLLECTION, entities);

      assertThat(timerFor("saveAll")).isNotNull();
      assertThat(timerFor("saveAll").count()).isEqualTo(1);
    }

    @Test
    void saveAllResultPassesThroughTimerWrapper() throws Exception {
      var entities = List.of(doc("a"), doc("b"));
      var result = template.saveAll(COLLECTION, entities);

      assertThat(result).containsExactlyElementsOf(entities);
    }
  }

  @Nested
  class DeleteByIdTimerRegistration {

    @Test
    void registersTimerAfterDeleteByIdOperation() throws Exception {
      template.deleteById(COLLECTION, "5");

      assertThat(timerFor("deleteById")).isNotNull();
      assertThat(timerFor("deleteById").count()).isEqualTo(1);
    }

    @Test
    void deleteByIdExceptionPropagatesThroughTimerWrapper() throws Exception {
      when(solrClient.deleteById(COLLECTION, "5")).thenThrow(new IOException("net error"));

      assertThatThrownBy(() -> template.deleteById(COLLECTION, "5"))
          .isInstanceOf(SolrException.class)
          .hasCauseInstanceOf(IOException.class);

      assertThat(timerFor("deleteById")).isNotNull();
    }
  }

  @Nested
  class DeleteByQueryTimerRegistration {

    @Test
    void registersTimerAfterDeleteByQueryOperation() throws Exception {
      template.deleteByQuery(COLLECTION, "status:inactive");

      assertThat(timerFor("deleteByQuery")).isNotNull();
      assertThat(timerFor("deleteByQuery").count()).isEqualTo(1);
    }
  }

  @Nested
  class QueryForHighlightPageTimerRegistration {

    @Test
    void registersTimerAfterQueryForHighlightPageOperation() throws Exception {
      var solrDoc = new SolrDocument();
      solrDoc.setField("id", "1");
      var docList = new SolrDocumentList();
      docList.add(solrDoc);
      docList.setNumFound(1L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of(doc("1")));
      when(response.getResults()).thenReturn(docList);
      when(response.getHighlighting()).thenReturn(Map.of());
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      template.queryForHighlightPage(COLLECTION, new SimpleQuery(Criteria.where("*").is("*")), TestDoc.class);

      assertThat(timerFor("queryForHighlightPage")).isNotNull();
      assertThat(timerFor("queryForHighlightPage").count()).isEqualTo(1);
    }
  }

  @Nested
  class QueryForFacetPageTimerRegistration {

    @Test
    void registersTimerAfterQueryForFacetPageOperation() throws Exception {
      var docList = new SolrDocumentList();
      docList.setNumFound(0L);

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of());
      when(response.getResults()).thenReturn(docList);
      when(response.getFacetFields()).thenReturn(List.of());
      when(response.getFacetQuery()).thenReturn(Map.of());
      when(solrClient.query(eq(COLLECTION), any(SolrParams.class))).thenReturn(response);

      template.queryForFacetPage(COLLECTION, new SimpleQuery(Criteria.where("*").is("*")), TestDoc.class);

      assertThat(timerFor("queryForFacetPage")).isNotNull();
      assertThat(timerFor("queryForFacetPage").count()).isEqualTo(1);
    }
  }

  @Nested
  class TimerTagsVerification {

    @Test
    void timerHasCorrectOperationAndCollectionTags() throws Exception {
      var solrQuery = new SolrQuery("*:*");
      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of());
      when(solrClient.query(COLLECTION, solrQuery)).thenReturn(response);

      template.query(COLLECTION, solrQuery, TestDoc.class);

      var timer = meterRegistry.find("solr.operations")
          .tag("operation", "query")
          .tag("collection", COLLECTION)
          .timer();

      assertThat(timer).isNotNull();
    }

    @Test
    void differentCollectionsProduceSeparateTimers() throws Exception {
      var solrQuery1 = new SolrQuery("*:*");
      var solrQuery2 = new SolrQuery("*:*");

      var response = mock(QueryResponse.class);
      when(response.getBeans(TestDoc.class)).thenReturn(List.of());
      when(solrClient.query(any(String.class), any(SolrParams.class))).thenReturn(response);

      template.query("books", solrQuery1, TestDoc.class);
      template.query("authors", solrQuery2, TestDoc.class);

      var booksTimer = meterRegistry.find("solr.operations")
          .tag("operation", "query")
          .tag("collection", "books")
          .timer();
      var authorsTimer = meterRegistry.find("solr.operations")
          .tag("operation", "query")
          .tag("collection", "authors")
          .timer();

      assertThat(booksTimer).isNotNull();
      assertThat(authorsTimer).isNotNull();
      assertThat(booksTimer.count()).isEqualTo(1);
      assertThat(authorsTimer.count()).isEqualTo(1);
    }
  }
}
