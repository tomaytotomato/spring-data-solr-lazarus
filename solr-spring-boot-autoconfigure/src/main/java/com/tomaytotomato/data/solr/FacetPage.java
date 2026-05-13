package com.tomaytotomato.data.solr;

import com.tomaytotomato.data.solr.query.FacetFieldEntry;
import com.tomaytotomato.data.solr.query.FacetQueryEntry;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FacetPage<T> extends PageImpl<T> {

  private final Map<String, List<FacetFieldEntry>> facetFields;
  private final List<FacetQueryEntry> facetQueries;

  public FacetPage(List<T> content, Pageable pageable, long total,
      Map<String, List<FacetFieldEntry>> facetFields,
      List<FacetQueryEntry> facetQueries) {
    super(content, pageable, total);
    this.facetFields = facetFields;
    this.facetQueries = facetQueries;
  }

  public Map<String, List<FacetFieldEntry>> getFacetFields() {
    return facetFields;
  }

  public List<FacetFieldEntry> getFacetField(String fieldName) {
    return facetFields.getOrDefault(fieldName, List.of());
  }

  public List<FacetQueryEntry> getFacetQueries() {
    return facetQueries;
  }
}
