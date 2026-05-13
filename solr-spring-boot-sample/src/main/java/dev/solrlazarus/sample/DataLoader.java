package dev.solrlazarus.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

  private final BookRepository bookRepository;

  public DataLoader(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  @Override
  public void run(String... args) {
    bookRepository.deleteAll();

    bookRepository.save(new Book("1", "Spring Boot in Action", "Craig Walls", 2016, 39.99, "technology"));
    bookRepository.save(new Book("2", "Spring in Action", "Craig Walls", 2022, 49.99, "technology"));
    bookRepository.save(new Book("3", "Effective Java", "Joshua Bloch", 2018, 45.00, "technology"));
    bookRepository.save(new Book("4", "Clean Code", "Robert C. Martin", 2008, 35.50, "technology"));
    bookRepository.save(new Book("5", "The Pragmatic Programmer", "David Thomas", 2019, 42.00, "technology"));
    bookRepository.save(new Book("6", "Dune", "Frank Herbert", 1965, 12.99, "fiction"));
    bookRepository.save(new Book("7", "Neuromancer", "William Gibson", 1984, 14.99, "fiction"));
    bookRepository.save(new Book("8", "Foundation", "Isaac Asimov", 1951, 11.99, "fiction"));
    bookRepository.save(new Book("9", "The Art of War", "Sun Tzu", -500, 9.99, "philosophy"));
    bookRepository.save(new Book("10", "Meditations", "Marcus Aurelius", 180, 8.99, "philosophy"));

    log.info("Loaded {} sample books into Solr", bookRepository.count());
  }
}
