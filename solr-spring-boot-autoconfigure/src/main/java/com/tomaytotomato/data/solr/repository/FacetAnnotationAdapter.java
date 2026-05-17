package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.query.FacetOptions;

/**
 * Converts a {@link Facet} annotation into a {@link FacetOptions} instance.
 */
class FacetAnnotationAdapter {

  private FacetAnnotationAdapter() {}

  static FacetOptions toFacetOptions(Facet annotation) {
    var options = new FacetOptions()
        .minCount(annotation.minCount())
        .limit(annotation.limit());
    for (var field : annotation.fields()) {
      options.addFacetOnField(field);
    }
    for (var query : annotation.queries()) {
      options.addFacetQuery(query);
    }
    return options;
  }
}
