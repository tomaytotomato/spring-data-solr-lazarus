package com.tomaytotomato.data.solr.query;

import java.util.ArrayList;
import java.util.List;

public class FacetOptions {

  private final List<String> facetFields = new ArrayList<>();
  private final List<String> facetQueries = new ArrayList<>();
  private int minCount = 1;
  private int limit = 10;
  private String sort;

  public FacetOptions addFacetOnField(String field) {
    facetFields.add(field);
    return this;
  }

  public FacetOptions addFacetQuery(String query) {
    facetQueries.add(query);
    return this;
  }

  public FacetOptions minCount(int min) {
    this.minCount = min;
    return this;
  }

  public FacetOptions limit(int limit) {
    this.limit = limit;
    return this;
  }

  public FacetOptions sort(String sort) {
    this.sort = sort;
    return this;
  }

  public List<String> getFacetFields() {
    return facetFields;
  }

  public List<String> getFacetQueries() {
    return facetQueries;
  }

  public int getMinCount() {
    return minCount;
  }

  public int getLimit() {
    return limit;
  }

  public String getSort() {
    return sort;
  }
}
