package com.tomaytotomato.data.solr.curator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

final class PricingStrategy {

  private static final Map<String, double[]> CATEGORY_PRICE_RANGES = Map.ofEntries(
      Map.entry("Computers", new double[]{29.99, 59.99}),
      Map.entry("Science", new double[]{24.99, 54.99}),
      Map.entry("Business", new double[]{19.99, 44.99}),
      Map.entry("Art", new double[]{25.99, 69.99}),
      Map.entry("Cooking", new double[]{18.99, 39.99}),
      Map.entry("History", new double[]{14.99, 34.99}),
      Map.entry("Fiction", new double[]{8.99, 24.99}),
      Map.entry("Science Fiction", new double[]{7.99, 19.99}),
      Map.entry("Fantasy", new double[]{7.99, 19.99}),
      Map.entry("Horror", new double[]{7.99, 18.99}),
      Map.entry("True Crime", new double[]{11.99, 26.99}),
      Map.entry("Poetry", new double[]{9.99, 22.99}),
      Map.entry("Biography", new double[]{12.99, 29.99}),
      Map.entry("Philosophy", new double[]{11.99, 27.99}),
      Map.entry("Psychology", new double[]{14.99, 39.99}),
      Map.entry("Religion", new double[]{10.99, 24.99})
  );

  private static final double[] DEFAULT_RANGE = {9.99, 34.99};

  private PricingStrategy() {}

  static double calculate(String primaryCategory, int pages, double rating) {
    var range = CATEGORY_PRICE_RANGES.getOrDefault(primaryCategory, DEFAULT_RANGE);
    var basePrice = range[0] + (range[1] - range[0]) * ThreadLocalRandom.current().nextDouble();

    var pageFactor = pages > 500 ? 1.15 : pages > 300 ? 1.05 : 1.0;
    var ratingFactor = rating >= 4.5 ? 1.10 : rating >= 4.0 ? 1.05 : 1.0;

    var finalPrice = basePrice * pageFactor * ratingFactor;

    return BigDecimal.valueOf(finalPrice)
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
