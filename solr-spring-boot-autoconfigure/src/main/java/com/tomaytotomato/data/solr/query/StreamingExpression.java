package com.tomaytotomato.data.solr.query;

public class StreamingExpression {

  private final String expression;

  private StreamingExpression(String expression) {
    this.expression = expression;
  }

  public static StreamingExpression of(String rawExpression) {
    return new StreamingExpression(rawExpression);
  }

  public static SearchBuilder search(String collection) {
    return new SearchBuilder(collection);
  }

  public String getExpression() {
    return expression;
  }

  public static class SearchBuilder {

    private final String collection;
    private String query = "*:*";
    private String fields;
    private String sort;
    private int rows = -1;

    SearchBuilder(String collection) {
      this.collection = collection;
    }

    public SearchBuilder query(String q) {
      this.query = q;
      return this;
    }

    public SearchBuilder fields(String... fl) {
      this.fields = String.join(",", fl);
      return this;
    }

    public SearchBuilder sort(String sort) {
      this.sort = sort;
      return this;
    }

    public SearchBuilder rows(int n) {
      this.rows = n;
      return this;
    }

    public StreamingExpression build() {
      var sb = new StringBuilder("search(").append(collection);
      sb.append(", q=\"").append(query).append("\"");
      if (fields != null) {
        sb.append(", fl=\"").append(fields).append("\"");
      }
      if (sort != null) {
        sb.append(", sort=\"").append(sort).append("\"");
      }
      if (rows > 0) {
        sb.append(", rows=").append(rows);
      }
      sb.append(")");
      return new StreamingExpression(sb.toString());
    }
  }
}
