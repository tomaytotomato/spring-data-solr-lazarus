package com.tomaytotomato.data.solr.query;

/**
 * Represents a geographic distance with a unit.
 *
 * <p>Solr's geospatial functions use kilometres for the {@code d} parameter.
 * Use {@link #toKilometers()} to obtain the normalised value regardless of the
 * unit the caller supplied.
 */
public record GeoDistance(double value, GeoDistanceUnit unit) {

  public enum GeoDistanceUnit {
    KILOMETERS, MILES
  }

  /** Creates a distance measured in kilometres. */
  public static GeoDistance kilometers(double km) {
    return new GeoDistance(km, GeoDistanceUnit.KILOMETERS);
  }

  /** Creates a distance measured in miles. */
  public static GeoDistance miles(double miles) {
    return new GeoDistance(miles, GeoDistanceUnit.MILES);
  }

  /**
   * Returns the distance in kilometres, converting from miles if necessary.
   */
  public double toKilometers() {
    return unit == GeoDistanceUnit.MILES ? value * 1.60934 : value;
  }
}
