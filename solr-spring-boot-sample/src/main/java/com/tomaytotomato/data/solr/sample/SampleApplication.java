package com.tomaytotomato.data.solr.sample;

import com.tomaytotomato.data.solr.repository.EnableSolrRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSolrRepositories
public class SampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
