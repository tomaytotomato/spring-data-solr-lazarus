package com.tomaytotomato.data.solr;

import java.util.List;

/**
 * Holds the result of a cursor-based deep paging query against Apache Solr.
 *
 * <p>Solr's cursorMark mechanism provides efficient traversal of large result sets without the
 * performance penalty of large offsets. Each response carries a {@code nextCursorMark} token that
 * must be supplied as the {@code cursorMark} parameter of the following request. When
 * {@code nextCursorMark} equals the request's {@code cursorMark}, the result set is exhausted.
 *
 * <p><strong>Solr requirement:</strong> the query's sort clause must include the uniqueKey field
 * (typically {@code id}), e.g. {@code title asc, id asc}. Solr will reject cursor requests that
 * lack a uniqueKey tie-breaker. This constraint is a Solr server requirement — it is not enforced
 * by this library.
 *
 * <p>The initial request should use {@code cursorMark=*} (available as
 * {@link org.apache.solr.common.params.CursorMarkParams#CURSOR_MARK_START}).
 *
 * @param <T> the document type
 */
public record CursorResult<T>(List<T> content, String cursorMark, boolean hasMore) {

  /**
   * Creates a {@code CursorResult} from a Solr query response.
   *
   * @param content           the documents returned by this page
   * @param requestCursorMark the cursorMark sent with the request
   * @param nextCursorMark    the cursorMark returned by Solr; {@code null} means no next mark
   * @return a {@code CursorResult} with {@code hasMore} set appropriately
   */
  public static <T> CursorResult<T> of(List<T> content, String requestCursorMark, String nextCursorMark) {
    boolean hasMore = nextCursorMark != null && !nextCursorMark.equals(requestCursorMark);
    return new CursorResult<>(content, nextCursorMark, hasMore);
  }

  /**
   * Returns an empty {@code CursorResult} representing a collection with no documents.
   */
  public static <T> CursorResult<T> empty() {
    return new CursorResult<>(List.of(), null, false);
  }
}
