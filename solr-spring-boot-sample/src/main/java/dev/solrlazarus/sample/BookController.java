package dev.solrlazarus.sample;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

  private final BookRepository bookRepository;

  public BookController(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  @GetMapping
  public Iterable<Book> findAll() {
    return bookRepository.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Book> findById(@PathVariable String id) {
    return bookRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public Book save(@RequestBody Book book) {
    return bookRepository.save(book);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteById(@PathVariable String id) {
    bookRepository.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/search")
  public List<Book> search(@RequestParam String q) {
    return bookRepository.findByTitleContaining(q);
  }

  @GetMapping("/by-author")
  public List<Book> findByAuthor(@RequestParam String author) {
    return bookRepository.findByAuthor(author);
  }

  @GetMapping("/by-author-after")
  public Page<Book> findByAuthorAfterYear(@RequestParam String author,
      @RequestParam int year, Pageable pageable) {
    return bookRepository.findByAuthorAndYearGreaterThan(author, year, pageable);
  }
}
