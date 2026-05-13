package com.tomaytotomato.data.solr.query;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoPointTest {

  @Nested
  class ToSolrString {

    @Test
    void formatsLatLonCommaSeparated() {
      var point = new GeoPoint(51.5074, -0.1278);
      assertThat(point.toSolrString()).isEqualTo("51.5074,-0.1278");
    }

    @Test
    void formatsZeroCoordinates() {
      var point = new GeoPoint(0.0, 0.0);
      assertThat(point.toSolrString()).isEqualTo("0.0,0.0");
    }

    @Test
    void formatsNegativeLatitude() {
      var point = new GeoPoint(-33.8688, 151.2093);
      assertThat(point.toSolrString()).isEqualTo("-33.8688,151.2093");
    }
  }

  @Nested
  class RecordAccessors {

    @Test
    void exposesLatitudeAndLongitude() {
      var point = new GeoPoint(48.8566, 2.3522);
      assertThat(point.latitude()).isEqualTo(48.8566);
      assertThat(point.longitude()).isEqualTo(2.3522);
    }
  }
}
