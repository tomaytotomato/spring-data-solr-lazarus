package com.tomaytotomato.data.solr.curator;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonPropertyOrder({
    "id", "title", "subtitle", "author", "description",
    "categories", "rating", "ratingsCount", "year",
    "pages", "price", "inStock", "latitude", "longitude", "locationName"
})
public record CuratedBook(
    String id,
    String title,
    String subtitle,
    String author,
    String description,
    List<String> categories,
    double rating,
    int ratingsCount,
    int year,
    int pages,
    double price,
    boolean inStock,
    double latitude,
    double longitude,
    String locationName
) {}
