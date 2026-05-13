package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.repository.SolrRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepository extends SolrRepository<Book, String> {

  List<Book> findByAuthor(String author);

  List<Book> findByTitleContaining(String title);

  Page<Book> findByAuthorAndYearGreaterThan(String author, int year, Pageable pageable);

  long countByAuthor(String author);

  boolean existsByTitle(String title);
}
