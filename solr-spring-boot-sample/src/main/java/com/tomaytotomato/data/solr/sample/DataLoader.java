package com.tomaytotomato.data.solr.sample;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

  private final BookRepository bookRepository;
  private final ObjectMapper objectMapper;

  public DataLoader(BookRepository bookRepository, ObjectMapper objectMapper) {
    this.bookRepository = bookRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(String... args) throws IOException {
    bookRepository.deleteAll();

    var resource = new ClassPathResource("curated-books.json");
    var jsonBooks = objectMapper.readValue(
        resource.getInputStream(), new TypeReference<List<JsonBook>>() {});

    var books = jsonBooks.stream().map(this::toBook).toList();
    books.forEach(bookRepository::save);

    log.info("Loaded {} curated books into Solr", books.size());
  }

  private Book toBook(JsonBook json) {
    var book = new Book();
    book.setId(json.id());
    book.setTitle(json.title());
    book.setSubtitle(json.subtitle());
    book.setAuthor(json.author());
    book.setDescription(json.description());
    book.setCategories(json.categories());
    book.setRating(json.rating());
    book.setRatingsCount(json.ratingsCount());
    book.setYear(json.year());
    book.setPages(json.pages());
    book.setPrice(json.price());
    book.setInStock(json.inStock());
    book.setLocation(json.latitude() + "," + json.longitude());
    book.setLocationName(json.locationName());
    return book;
  }

  record JsonBook(
      String id, String title, String subtitle, String author,
      String description, List<String> categories, double rating,
      int ratingsCount, int year, int pages, double price,
      boolean inStock, double latitude, double longitude, String locationName
  ) {}
}
