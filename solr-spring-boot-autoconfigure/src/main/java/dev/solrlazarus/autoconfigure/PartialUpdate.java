package dev.solrlazarus.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.solr.common.SolrInputDocument;

public class PartialUpdate {

  private final String idField;
  private final String idValue;
  private final Map<String, Map<String, Object>> updates = new LinkedHashMap<>();

  public PartialUpdate(String idValue) {
    this("id", idValue);
  }

  public PartialUpdate(String idField, String idValue) {
    this.idField = idField;
    this.idValue = idValue;
  }

  public PartialUpdate set(String field, Object value) {
    updates.put(field, Map.of("set", value));
    return this;
  }

  public PartialUpdate add(String field, Object value) {
    updates.put(field, Map.of("add", value));
    return this;
  }

  public PartialUpdate increment(String field, Number value) {
    updates.put(field, Map.of("inc", value));
    return this;
  }

  public String getIdField() {
    return idField;
  }

  public String getIdValue() {
    return idValue;
  }

  public SolrInputDocument toSolrInputDocument() {
    var doc = new SolrInputDocument();
    doc.addField(idField, idValue);
    updates.forEach(doc::addField);
    return doc;
  }
}
