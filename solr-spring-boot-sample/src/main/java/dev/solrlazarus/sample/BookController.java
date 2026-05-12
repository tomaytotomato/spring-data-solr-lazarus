package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.SolrPage;
import dev.solrlazarus.autoconfigure.SolrTemplate;
import dev.solrlazarus.autoconfigure.query.Criteria;
import dev.solrlazarus.autoconfigure.query.SimpleQuery;
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
  private final SolrTemplate solrTemplate;

  public BookController(BookRepository bookRepository, SolrTemplate solrTemplate) {
    this.bookRepository = bookRepository;
    this.solrTemplate = solrTemplate;
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
  public SolrPage<Book> search(@RequestParam String q) {
    var criteria = Criteria.where("title").contains(q);
    var query = new SimpleQuery(criteria);
    return solrTemplate.queryForPage("books", query, Book.class);
  }
}
