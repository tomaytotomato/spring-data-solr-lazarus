package dev.solrlazarus.autoconfigure;

import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class SolrPage<T> extends PageImpl<T> {

  private final Float maxScore;

  public SolrPage(List<T> content, Pageable pageable, long total, Float maxScore) {
    super(content, pageable, total);
    this.maxScore = maxScore;
  }

  public static <T> SolrPage<T> of(List<T> content, Pageable pageable, long numFound, Float maxScore) {
    return new SolrPage<>(content, pageable, numFound, maxScore);
  }

  public Float getMaxScore() {
    return maxScore;
  }
}
