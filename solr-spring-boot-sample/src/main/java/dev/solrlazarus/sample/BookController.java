package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.CursorResult;
import dev.solrlazarus.autoconfigure.FacetPage;
import dev.solrlazarus.autoconfigure.HighlightPage;
import dev.solrlazarus.autoconfigure.SolrTemplate;
import dev.solrlazarus.autoconfigure.query.Criteria;
import dev.solrlazarus.autoconfigure.query.FacetOptions;
import dev.solrlazarus.autoconfigure.query.HighlightOptions;
import dev.solrlazarus.autoconfigure.query.SimpleQuery;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  // --- CRUD ---

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

  // --- Derived Queries ---

  @GetMapping("/search")
  public List<Book> search(@RequestParam String q) {
    return bookRepository.findByTitleContaining(q);
  }

  @GetMapping("/by-author")
  public List<Book> findByAuthor(@RequestParam String author) {
    return bookRepository.findByAuthor(author);
  }

  @GetMapping("/by-genre")
  public List<Book> findByGenre(@RequestParam String genre) {
    return bookRepository.findByGenre(genre);
  }

  @GetMapping("/by-author-after")
  public Page<Book> findByAuthorAfterYear(@RequestParam String author,
      @RequestParam int year, Pageable pageable) {
    return bookRepository.findByAuthorAndYearGreaterThan(author, year, pageable);
  }

  @GetMapping("/expensive")
  public List<Book> findExpensive(@RequestParam(defaultValue = "40") double minPrice) {
    return bookRepository.findByPriceGreaterThan(minPrice);
  }

  @GetMapping("/price-range")
  public List<Book> findByPriceRange(@RequestParam double low, @RequestParam double high) {
    return bookRepository.findByPriceBetween(low, high);
  }

  // --- @Query Annotation ---

  @GetMapping("/custom-search")
  public List<Book> customSearch(@RequestParam String title, @RequestParam String author) {
    return bookRepository.findByTitleAndAuthorCustom(title, author);
  }

  @GetMapping("/genre-price")
  public List<Book> findByGenreAndPrice(@RequestParam String genre,
      @RequestParam double low, @RequestParam double high) {
    return bookRepository.findByGenreAndPriceRange(genre, low, high);
  }

  @GetMapping("/count-genre")
  public Map<String, Long> countByGenre(@RequestParam String genre) {
    return Map.of("genre", (long) genre.length(), "count", bookRepository.countByGenreCustom(genre));
  }

  // --- Highlighting ---

  @GetMapping("/highlight")
  public HighlightPage<Book> searchWithHighlights(@RequestParam String q) {
    var query = new SimpleQuery(Criteria.where("title").contains(q));
    query.setPageable(PageRequest.of(0, 10));
    query.setHighlightOptions(new HighlightOptions()
        .addField("title")
        .preTag("<b>")
        .postTag("</b>")
        .snippets(2)
        .fragsize(200));
    return solrTemplate.queryForHighlightPage("books", query, Book.class);
  }

  // --- Faceting ---

  @GetMapping("/facets")
  public FacetPage<Book> searchWithFacets(
      @RequestParam(defaultValue = "*") String q) {
    var criteria = q.equals("*") ? Criteria.where("*").expression("*") : Criteria.where("title").contains(q);
    var query = new SimpleQuery(criteria);
    query.setPageable(PageRequest.of(0, 10));
    query.setFacetOptions(new FacetOptions()
        .addFacetOnField("genre")
        .addFacetOnField("author")
        .minCount(1)
        .limit(20));
    return solrTemplate.queryForFacetPage("books", query, Book.class);
  }

  // --- Cursor-based Deep Paging ---

  @GetMapping("/cursor")
  public CursorResult<Book> cursorPage(
      @RequestParam(defaultValue = "*") String cursorMark,
      @RequestParam(defaultValue = "3") int pageSize) {
    var query = new SimpleQuery(Criteria.where("*").expression("*"));
    query.setCursorMark(cursorMark);
    query.setPageable(PageRequest.of(0, pageSize, Sort.by("id").ascending()));
    return solrTemplate.queryWithCursor("books", query, Book.class);
  }

  // --- Statistics ---

  @GetMapping("/stats")
  public Map<String, Object> stats() {
    return Map.of(
        "totalBooks", bookRepository.count(),
        "authorCount", bookRepository.countByAuthor("Craig Walls"),
        "hasSpringInAction", bookRepository.existsByTitle("Spring in Action"));
  }
}
