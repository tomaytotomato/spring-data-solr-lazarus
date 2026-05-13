package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.repository.EnableSolrRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSolrRepositories
public class SampleApplication {

  static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
