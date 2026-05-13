package dev.solrlazarus.autoconfigure.query;

import java.util.ArrayList;
import java.util.List;

public class HighlightOptions {

  private final List<String> fields = new ArrayList<>();
  private String preTag = "<em>";
  private String postTag = "</em>";
  private int snippets = 1;
  private int fragsize = 100;

  public HighlightOptions addField(String field) {
    fields.add(field);
    return this;
  }

  public HighlightOptions preTag(String tag) {
    this.preTag = tag;
    return this;
  }

  public HighlightOptions postTag(String tag) {
    this.postTag = tag;
    return this;
  }

  public HighlightOptions snippets(int n) {
    this.snippets = n;
    return this;
  }

  public HighlightOptions fragsize(int size) {
    this.fragsize = size;
    return this;
  }

  public List<String> getFields() {
    return fields;
  }

  public String getPreTag() {
    return preTag;
  }

  public String getPostTag() {
    return postTag;
  }

  public int getSnippets() {
    return snippets;
  }

  public int getFragsize() {
    return fragsize;
  }
}
