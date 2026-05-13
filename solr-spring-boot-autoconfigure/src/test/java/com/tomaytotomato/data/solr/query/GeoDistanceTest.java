package com.tomaytotomato.data.solr.query;

import com.tomaytotomato.data.solr.query.GeoDistance.GeoDistanceUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoDistanceTest {

  @Nested
  class FactoryMethods {

    @Test
    void kilometersFactoryCreatesKilometerDistance() {
      var distance = GeoDistance.kilometers(10.0);
      assertThat(distance.value()).isEqualTo(10.0);
      assertThat(distance.unit()).isEqualTo(GeoDistanceUnit.KILOMETERS);
    }

    @Test
    void milesFactoryCreatesMileDistance() {
      var distance = GeoDistance.miles(5.0);
      assertThat(distance.value()).isEqualTo(5.0);
      assertThat(distance.unit()).isEqualTo(GeoDistanceUnit.MILES);
    }
  }

  @Nested
  class ToKilometers {

    @Test
    void kilometersReturnValueUnchanged() {
      var distance = GeoDistance.kilometers(25.0);
      assertThat(distance.toKilometers()).isEqualTo(25.0);
    }

    @Test
    void milesConvertsToKilometers() {
      var distance = GeoDistance.miles(1.0);
      assertThat(distance.toKilometers()).isCloseTo(1.60934, within(0.00001));
    }

    @Test
    void tenMilesConvertsCorrectly() {
      var distance = GeoDistance.miles(10.0);
      assertThat(distance.toKilometers()).isCloseTo(16.0934, within(0.0001));
    }
  }
}
