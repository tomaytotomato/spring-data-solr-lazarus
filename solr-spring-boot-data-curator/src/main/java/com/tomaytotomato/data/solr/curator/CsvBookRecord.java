package com.tomaytotomato.data.solr.curator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CsvBookRecord(
    @JsonProperty("isbn13") String isbn13,
    @JsonProperty("isbn10") String isbn10,
    @JsonProperty("title") String title,
    @JsonProperty("subtitle") String subtitle,
    @JsonProperty("authors") String authors,
    @JsonProperty("categories") String categories,
    @JsonProperty("thumbnail") String thumbnail,
    @JsonProperty("description") String description,
    @JsonProperty("published_year") String publishedYear,
    @JsonProperty("average_rating") String averageRating,
    @JsonProperty("num_pages") String numPages,
    @JsonProperty("ratings_count") String ratingsCount
) {

  boolean hasRichMetadata() {
    return isNotBlank(description)
        && isNotBlank(categories)
        && isNotBlank(averageRating)
        && isNotBlank(publishedYear)
        && isNotBlank(authors);
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }
}
