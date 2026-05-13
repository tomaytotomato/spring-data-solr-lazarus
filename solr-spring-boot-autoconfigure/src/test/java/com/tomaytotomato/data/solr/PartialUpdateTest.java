package com.tomaytotomato.data.solr;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartialUpdateTest {

  @Nested
  class Construction {

    @Test
    void createsWithDefaultIdField() {
      var update = new PartialUpdate("abc-123");

      assertThat(update.getIdField()).isEqualTo("id");
      assertThat(update.getIdValue()).isEqualTo("abc-123");
    }

    @Test
    void createsWithCustomIdField() {
      var update = new PartialUpdate("sku", "WIDGET-42");

      assertThat(update.getIdField()).isEqualTo("sku");
      assertThat(update.getIdValue()).isEqualTo("WIDGET-42");
    }
  }

  @Nested
  class SetOperation {

    @Test
    void producesCorrectSolrInputDocument() {
      var update = new PartialUpdate("doc-1").set("title", "New Title");
      var doc = update.toSolrInputDocument();

      assertThat(doc.getField("id").getValue()).isEqualTo("doc-1");
      var titleField = doc.getField("title").getValue();
      assertThat(titleField).isInstanceOf(java.util.Map.class);
      @SuppressWarnings("unchecked")
      var titleMap = (java.util.Map<String, Object>) titleField;
      assertThat(titleMap).containsEntry("set", "New Title");
    }
  }

  @Nested
  class AddOperation {

    @Test
    void producesCorrectSolrInputDocument() {
      var update = new PartialUpdate("doc-2").add("tags", "spring");
      var doc = update.toSolrInputDocument();

      assertThat(doc.getField("id").getValue()).isEqualTo("doc-2");
      var tagsField = doc.getField("tags").getValue();
      assertThat(tagsField).isInstanceOf(java.util.Map.class);
      @SuppressWarnings("unchecked")
      var tagsMap = (java.util.Map<String, Object>) tagsField;
      assertThat(tagsMap).containsEntry("add", "spring");
    }
  }

  @Nested
  class IncrementOperation {

    @Test
    void producesCorrectSolrInputDocument() {
      var update = new PartialUpdate("doc-3").increment("viewCount", 1);
      var doc = update.toSolrInputDocument();

      assertThat(doc.getField("id").getValue()).isEqualTo("doc-3");
      var viewField = doc.getField("viewCount").getValue();
      assertThat(viewField).isInstanceOf(java.util.Map.class);
      @SuppressWarnings("unchecked")
      var viewMap = (java.util.Map<String, Object>) viewField;
      assertThat(viewMap).containsEntry("inc", 1);
    }
  }

  @Nested
  class MultipleOperations {

    @Test
    void multipleOperationsOnSameDocument() {
      var update = new PartialUpdate("doc-4")
          .set("title", "Updated")
          .add("tags", "solr")
          .increment("viewCount", 5);

      var doc = update.toSolrInputDocument();

      assertThat(doc.getField("id").getValue()).isEqualTo("doc-4");
      assertThat(doc.getField("title")).isNotNull();
      assertThat(doc.getField("tags")).isNotNull();
      assertThat(doc.getField("viewCount")).isNotNull();
    }

    @Test
    void operationsChainsFluentlyReturningPartialUpdate() {
      var update = new PartialUpdate("doc-5");
      var result = update.set("field1", "val1").add("field2", "val2").increment("field3", 3);

      assertThat(result).isSameAs(update);
    }
  }
}
