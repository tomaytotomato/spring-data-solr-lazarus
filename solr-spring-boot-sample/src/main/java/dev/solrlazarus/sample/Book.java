package dev.solrlazarus.sample;

import dev.solrlazarus.autoconfigure.mapping.SolrDocument;
import org.apache.solr.client.solrj.beans.Field;

@SolrDocument(collection = "books")
public class Book {

  @Field
  private String id;

  @Field
  private String title;

  @Field
  private String author;

  @Field("publication_year")
  private int year;

  public Book() {}

  public Book(String id, String title, String author, int year) {
    this.id = id;
    this.title = title;
    this.author = author;
    this.year = year;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }
}
