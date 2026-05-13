package com.tomaytotomato.data.solr;

import com.tomaytotomato.data.solr.query.HighlightEntry;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class HighlightPage<T> extends PageImpl<T> {

  private final List<HighlightEntry<T>> highlighted;

  public HighlightPage(List<T> content, Pageable pageable, long total,
      List<HighlightEntry<T>> highlighted) {
    super(content, pageable, total);
    this.highlighted = highlighted;
  }

  public List<HighlightEntry<T>> getHighlighted() {
    return highlighted;
  }
}
