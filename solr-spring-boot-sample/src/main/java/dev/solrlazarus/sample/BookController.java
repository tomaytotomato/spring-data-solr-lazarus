package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.SolrTemplate;
import java.util.List;
import java.util.Optional;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

  private final SolrTemplate solrTemplate;

  public BookController(SolrTemplate solrTemplate) {
    this.solrTemplate = solrTemplate;
  }

  @GetMapping
  public List<Book> findAll() {
    return solrTemplate.query("books", new SolrQuery("*:*"), Book.class);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Book> findById(@PathVariable String id) {
    var book = solrTemplate.findById("books", id, Book.class);
    return book.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public Book save(@RequestBody Book book) {
    var saved = solrTemplate.save("books", book);
    solrTemplate.commit("books");
    return saved;
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteById(@PathVariable String id) {
    solrTemplate.deleteById("books", id);
    solrTemplate.commit("books");
    return ResponseEntity.noContent().build();
  }
}
