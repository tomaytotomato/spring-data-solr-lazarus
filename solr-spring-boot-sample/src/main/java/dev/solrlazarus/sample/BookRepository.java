package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.repository.Query;
import dev.solrlazarus.autoconfigure.repository.SolrRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepository extends SolrRepository<Book, String> {

  List<Book> findByAuthor(String author);

  List<Book> findByTitleContaining(String title);

  List<Book> findByGenre(String genre);

  List<Book> findByPriceGreaterThan(double price);

  List<Book> findByPriceBetween(double low, double high);

  Page<Book> findByAuthorAndYearGreaterThan(String author, int year, Pageable pageable);

  long countByAuthor(String author);

  boolean existsByTitle(String title);

  @Query("title:?0 AND author:?1")
  List<Book> findByTitleAndAuthorCustom(String title, String author);

  @Query("genre:?0 AND price:[?1 TO ?2]")
  List<Book> findByGenreAndPriceRange(String genre, double low, double high);

  @Query(value = "genre:?0", count = true)
  long countByGenreCustom(String genre);
}
