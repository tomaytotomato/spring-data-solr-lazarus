package com.tomaytotomato.data.solr.query;

/**
 * Represents a geographic point as a latitude/longitude coordinate pair.
 *
 * <p>Used with {@link Criteria#near(GeoPoint, GeoDistance)} and
 * {@link Criteria#within(GeoPoint, GeoDistance)} to build Solr geospatial function queries.
 */
public record GeoPoint(double latitude, double longitude) {

  /**
   * Returns the coordinate pair formatted for Solr's {@code pt} parameter: {@code lat,lon}.
   */
  public String toSolrString() {
    return latitude + "," + longitude;
  }
}
