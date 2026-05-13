package dev.solrlazarus.autoconfigure.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

import static org.assertj.core.api.Assertions.assertThat;

class SolrQueryCreatorTest {

  static class Product {
    String title;
    String author;
    double price;
    boolean inStock;
    String category;
  }

  private String createQuery(String methodName, Object... params) {
    var tree = new PartTree(methodName, Product.class);
    var accessor = new StubParameterAccessor(params);
    var creator = new SolrQueryCreator(tree, accessor);
    return creator.createQuery().toSolrQuery().getQuery();
  }

  @Nested
  class SimpleProperty {

    @Test
    void findByTitle() {
      assertThat(createQuery("findByTitle", "Spring Boot"))
          .isEqualTo("title:Spring\\ Boot");
    }

    @Test
    void findByTitleAndAuthor() {
      assertThat(createQuery("findByTitleAndAuthor", "Spring", "Picard"))
          .isEqualTo("title:Spring AND author:Picard");
    }
  }

  @Nested
  class NegatingSimpleProperty {

    @Test
    void findByTitleNot() {
      assertThat(createQuery("findByTitleNot", "Obsolete"))
          .isEqualTo("-title:Obsolete");
    }
  }

  @Nested
  class Containing {

    @Test
    void findByTitleContaining() {
      assertThat(createQuery("findByTitleContaining", "spring"))
          .isEqualTo("title:*spring*");
    }
  }

  @Nested
  class NotContaining {

    @Test
    void findByTitleNotContaining() {
      assertThat(createQuery("findByTitleNotContaining", "draft"))
          .isEqualTo("-title:*draft*");
    }
  }

  @Nested
  class StartingWith {

    @Test
    void findByTitleStartingWith() {
      assertThat(createQuery("findByTitleStartingWith", "Spring"))
          .isEqualTo("title:Spring*");
    }
  }

  @Nested
  class EndingWith {

    @Test
    void findByTitleEndingWith() {
      assertThat(createQuery("findByTitleEndingWith", "Boot"))
          .isEqualTo("title:*Boot");
    }
  }

  @Nested
  class GreaterThan {

    @Test
    void findByPriceGreaterThan() {
      assertThat(createQuery("findByPriceGreaterThan", 10.0))
          .isEqualTo("price:{10.0 TO *]");
    }
  }

  @Nested
  class GreaterThanEqual {

    @Test
    void findByPriceGreaterThanEqual() {
      assertThat(createQuery("findByPriceGreaterThanEqual", 10.0))
          .isEqualTo("price:[10.0 TO *]");
    }
  }

  @Nested
  class LessThan {

    @Test
    void findByPriceLessThan() {
      assertThat(createQuery("findByPriceLessThan", 100.0))
          .isEqualTo("price:[* TO 100.0}");
    }
  }

  @Nested
  class LessThanEqual {

    @Test
    void findByPriceLessThanEqual() {
      assertThat(createQuery("findByPriceLessThanEqual", 100.0))
          .isEqualTo("price:[* TO 100.0]");
    }
  }

  @Nested
  class Between {

    @Test
    void findByPriceBetween() {
      assertThat(createQuery("findByPriceBetween", 10.0, 50.0))
          .isEqualTo("price:[10.0 TO 50.0]");
    }
  }

  @Nested
  class In {

    @Test
    void findByCategoryIn() {
      assertThat(createQuery("findByCategoryIn", List.of("books", "ebooks")))
          .isEqualTo("category:(books OR ebooks)");
    }
  }

  @Nested
  class IsNull {

    @Test
    void findByAuthorIsNull() {
      assertThat(createQuery("findByAuthorIsNull"))
          .isEqualTo("-author:[* TO *]");
    }
  }

  @Nested
  class IsNotNull {

    @Test
    void findByAuthorIsNotNull() {
      assertThat(createQuery("findByAuthorIsNotNull"))
          .isEqualTo("author:[* TO *]");
    }
  }

  @Nested
  class IsTrue {

    @Test
    void findByInStockTrue() {
      assertThat(createQuery("findByInStockTrue"))
          .isEqualTo("inStock:true");
    }
  }

  @Nested
  class IsFalse {

    @Test
    void findByInStockFalse() {
      assertThat(createQuery("findByInStockFalse"))
          .isEqualTo("inStock:false");
    }
  }

  @Nested
  class OrQueries {

    @Test
    void findByTitleOrAuthor() {
      assertThat(createQuery("findByTitleOrAuthor", "Spring", "Picard"))
          .isEqualTo("title:Spring OR author:Picard");
    }
  }

  @Nested
  class ComplexQueries {

    @Test
    void findByTitleAndPriceGreaterThan() {
      assertThat(createQuery("findByTitleAndPriceGreaterThan", "Spring", 10.0))
          .isEqualTo("title:Spring AND price:{10.0 TO *]");
    }

    @Test
    void findByTitleContainingOrAuthorStartingWith() {
      assertThat(createQuery("findByTitleContainingOrAuthorStartingWith", "spring", "Pic"))
          .isEqualTo("title:*spring* OR author:Pic*");
    }
  }

  @Nested
  class Sorting {

    @Test
    void appliesSortFromAccessor() {
      var tree = new PartTree("findByTitle", Product.class);
      var accessor = new StubParameterAccessor(Sort.by("price").descending(), "Spring");
      var creator = new SolrQueryCreator(tree, accessor);
      var solrQuery = creator.createQuery().toSolrQuery();

      assertThat(solrQuery.getQuery()).isEqualTo("title:Spring");
      assertThat(solrQuery.get("sort")).isEqualTo("price desc");
    }
  }

  static class StubParameterAccessor implements org.springframework.data.repository.query.ParameterAccessor {

    private final Object[] values;
    private final Sort sort;

    StubParameterAccessor(Object... values) {
      this(Sort.unsorted(), values);
    }

    StubParameterAccessor(Sort sort, Object... values) {
      this.sort = sort;
      this.values = values;
    }

    @Override
    public org.springframework.data.domain.ScrollPosition getScrollPosition() {
      return org.springframework.data.domain.ScrollPosition.keyset();
    }

    @Override
    public org.springframework.data.domain.Pageable getPageable() {
      return org.springframework.data.domain.Pageable.unpaged();
    }

    @Override
    public Sort getSort() {
      return sort;
    }

    @Override
    public org.springframework.data.domain.Limit getLimit() {
      return org.springframework.data.domain.Limit.unlimited();
    }

    @Override
    public Class<?> findDynamicProjection() {
      return null;
    }

    @Override
    public Object getBindableValue(int index) {
      return values[index];
    }

    @Override
    public boolean hasBindableNullValue() {
      return false;
    }

    @Override
    public Iterator<Object> iterator() {
      return List.of(values).iterator();
    }
  }
}
