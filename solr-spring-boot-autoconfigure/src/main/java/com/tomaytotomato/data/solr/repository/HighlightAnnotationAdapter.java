package com.tomaytotomato.data.solr.repository;

import com.tomaytotomato.data.solr.query.HighlightOptions;

/**
 * Converts a {@link Highlight} annotation into a {@link HighlightOptions} instance.
 */
class HighlightAnnotationAdapter {

  private HighlightAnnotationAdapter() {}

  static HighlightOptions toHighlightOptions(Highlight annotation) {
    var options = new HighlightOptions()
        .preTag(annotation.prefix())
        .postTag(annotation.postfix())
        .snippets(annotation.snippets())
        .fragsize(annotation.fragsize());
    for (var field : annotation.fields()) {
      options.addField(field);
    }
    return options;
  }
}
