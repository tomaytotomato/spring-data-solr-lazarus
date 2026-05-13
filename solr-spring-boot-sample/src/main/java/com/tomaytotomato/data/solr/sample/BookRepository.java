package com.tomaytotomato.data.solr.sample;

import com.tomaytotomato.data.solr.repository.Query;
import com.tomaytotomato.data.solr.repository.SolrRepository;
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

  @Query("title_s:?0 AND author_s:?1")
  List<Book> findByTitleAndAuthorCustom(String title, String author);

  @Query("genre_s:?0 AND price_d:[?1 TO ?2]")
  List<Book> findByGenreAndPriceRange(String genre, double low, double high);

  @Query(value = "genre_s:?0", count = true)
  long countByGenreCustom(String genre);
}
