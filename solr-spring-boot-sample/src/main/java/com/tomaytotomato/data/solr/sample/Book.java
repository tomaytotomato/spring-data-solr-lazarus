package com.tomaytotomato.data.solr.sample;

import com.tomaytotomato.data.solr.mapping.SolrDocument;
import org.apache.solr.client.solrj.beans.Field;

@SolrDocument(collection = "books")
public class Book {

  @Field
  private String id;

  @Field("title_s")
  private String title;

  @Field("author_s")
  private String author;

  @Field("year_i")
  private int year;

  @Field("price_d")
  private double price;

  @Field("genre_s")
  private String genre;

  public Book() {}

  public Book(String id, String title, String author, int year, double price, String genre) {
    this.id = id;
    this.title = title;
    this.author = author;
    this.year = year;
    this.price = price;
    this.genre = genre;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }

  public int getYear() { return year; }
  public void setYear(int year) { this.year = year; }

  public double getPrice() { return price; }
  public void setPrice(double price) { this.price = price; }

  public String getGenre() { return genre; }
  public void setGenre(String genre) { this.genre = genre; }
}
