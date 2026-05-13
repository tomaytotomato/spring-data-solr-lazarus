package com.tomaytotomato.data.solr.query;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingExpressionTest {

  @Nested
  class RawExpression {

    @Test
    void passesThroughUnchanged() {
      var expression = StreamingExpression.of("search(books, q=\"*:*\", fl=\"id,title\", sort=\"id asc\", rows=10)");
      assertThat(expression.getExpression())
          .isEqualTo("search(books, q=\"*:*\", fl=\"id,title\", sort=\"id asc\", rows=10)");
    }

    @Test
    void preservesArbitraryStreamingExpression() {
      var raw = "facet(books, q=\"*:*\", buckets=\"genre\", bucketSorts=\"count(*) desc\", bucketSizeLimit=10, count(*))";
      var expression = StreamingExpression.of(raw);
      assertThat(expression.getExpression()).isEqualTo(raw);
    }
  }

  @Nested
  class SearchBuilder {

    @Test
    void buildsMinimalSearchExpressionWithDefaults() {
      var expression = StreamingExpression.search("books").build();
      assertThat(expression.getExpression())
          .isEqualTo("search(books, q=\"*:*\")");
    }

    @Test
    void buildsSearchExpressionWithAllParameters() {
      var expression = StreamingExpression.search("books")
          .query("author:Picard")
          .fields("title", "author")
          .sort("title asc")
          .rows(50)
          .build();
      assertThat(expression.getExpression())
          .isEqualTo("search(books, q=\"author:Picard\", fl=\"title,author\", sort=\"title asc\", rows=50)");
    }

    @Test
    void buildsSearchExpressionWithQueryOnly() {
      var expression = StreamingExpression.search("articles")
          .query("category:news")
          .build();
      assertThat(expression.getExpression())
          .isEqualTo("search(articles, q=\"category:news\")");
    }

    @Test
    void buildsSearchExpressionWithFieldsAndSort() {
      var expression = StreamingExpression.search("products")
          .fields("id", "name", "price")
          .sort("price desc")
          .build();
      assertThat(expression.getExpression())
          .isEqualTo("search(products, q=\"*:*\", fl=\"id,name,price\", sort=\"price desc\")");
    }

    @Test
    void omitsRowsWhenNotSet() {
      var expression = StreamingExpression.search("books")
          .query("*:*")
          .fields("id")
          .build();
      assertThat(expression.getExpression())
          .doesNotContain("rows=");
    }

    @Test
    void omitsFieldsWhenNotSet() {
      var expression = StreamingExpression.search("books")
          .query("title:Spring")
          .sort("id asc")
          .build();
      assertThat(expression.getExpression())
          .doesNotContain("fl=");
    }

    @Test
    void omitsSortWhenNotSet() {
      var expression = StreamingExpression.search("books")
          .query("title:Spring")
          .rows(10)
          .build();
      assertThat(expression.getExpression())
          .doesNotContain("sort=");
    }
  }
}
